import { type CSSProperties, type ReactNode, useState } from "react";
import { Cog } from "lucide-react";
import { SidebarInset, SidebarProvider, SidebarTrigger, useSidebar } from "@/components/ui/sidebar.tsx";
import { cn } from "@/lib/utils.ts";
import { EmptyCard } from "@/common/EmptyCard.tsx";
import { SidebarResizeHandle } from "@/common/SidebarResizeHandle.tsx";
import ModulesTreeSection from "@/sections/modulesTreeSection/ModulesTreeSection.tsx";
import ModuleDetailSection from "@/sections/moduleDetailSection/ModuleDetailSection.tsx";
import type { UseFetchResult } from "@/hooks/useFetch.ts";
import { executeModules } from "@/common/executeModules.ts";
import ErrorDialog from "@/dialogs/error/ErrorDialog.tsx";
import type { Settings } from "@/dialogs/settings/settingsTypes.ts";
import { useFavorites } from "@/hooks/useFavorites.ts";
import { useSavedSequences } from "@/hooks/useSavedSequences.ts";
import { useModuleSelection } from "@/hooks/useModuleSelection.ts";
import type { ModuleMetadata } from "@/types/moduleMetadata.ts";
import { getLatestJobForModule, type Job } from "@/types/job.ts";

const SIDEBAR_WIDTH_STORAGE_KEY = "sidebar_width";
const SIDEBAR_DEFAULT_WIDTH = 256;
const SIDEBAR_MIN_WIDTH = 200;
const SIDEBAR_MAX_WIDTH = 600;

type RunnerViewProps = {
    modulesResult: UseFetchResult<ModuleMetadata[]>;
    jobsResult: UseFetchResult<Job[]>;
    settingsResult: UseFetchResult<Settings>;
    headerActions: ReactNode;
};

export default function RunnerView({ modulesResult, jobsResult, settingsResult, headerActions }: RunnerViewProps) {
    const selection = useModuleSelection();
    const favorites = useFavorites();
    const savedSequences = useSavedSequences();

    const selectedModule =
        modulesResult.status === "ok"
            ? modulesResult.data.find((module) => module.id === selection.currentModuleId)
            : undefined;
    const jobs = jobsResult.status === "ok" ? jobsResult.data : [];
    const latestJob = selectedModule && getLatestJobForModule(jobs, selectedModule);
    const shownJob = jobs.find((job) => job.id === selection.jobId && job.moduleId === selectedModule?.id) ?? latestJob;

    const [executeError, setExecuteError] = useState<string>();
    const [executingSelection, setExecutingSelection] = useState(false);

    const executeModule = async (module: ModuleMetadata) => {
        selection.select(module);
        if (jobsResult.status === "ok") {
            setExecuteError(await executeModules([module.id], jobsResult.setData));
        }
    };

    const executeRun = async (moduleIds: string[]) => {
        if (jobsResult.status !== "ok") {
            return;
        }
        setExecutingSelection(true);
        let followedJobId = selection.jobId;
        try {
            setExecuteError(
                await executeModules(moduleIds, jobsResult.setData, {
                    onJobStarted: (_, job) => {
                        selection.followJob(job, followedJobId);
                        followedJobId = job.id;
                    },
                }),
            );
        } finally {
            setExecutingSelection(false);
        }
    };

    const [sidebarWidth, setSidebarWidth] = useState(loadSidebarWidth);
    const [resizingSidebar, setResizingSidebar] = useState(false);

    const onSidebarResizingChange = (resizing: boolean) => {
        setResizingSidebar(resizing);
        if (!resizing) {
            saveSidebarWidth(sidebarWidth);
        }
    };

    return (
        <SidebarProvider
            style={{ "--sidebar-width": `${sidebarWidth}px` } as CSSProperties}
            className={cn(
                resizingSidebar &&
                    "cursor-col-resize select-none **:data-[slot=sidebar-container]:transition-none **:data-[slot=sidebar-gap]:transition-none",
            )}
        >
            <ModulesTreeSection
                modulesResult={modulesResult}
                jobsResult={jobsResult}
                sourceFormat={settingsResult.status === "ok" ? settingsResult.data.sourceFormat : undefined}
                shownJobId={shownJob?.id}
                selection={selection}
                favorites={favorites}
                savedSequences={savedSequences}
                executingSelection={executingSelection}
                onExecute={executeModule}
                onExecuteSelection={() => executeRun(selection.moduleIds)}
                onExecuteRun={executeRun}
            />
            <ErrorDialog
                title="Module could not be started"
                error={executeError}
                onClose={() => setExecuteError(undefined)}
            />
            <SidebarResizeHandle
                onResize={(width) => setSidebarWidth(clampSidebarWidth(width))}
                onResizingChange={onSidebarResizingChange}
            />
            <SidebarInset className="h-svh">
                <header className="flex h-12 shrink-0 items-center gap-2 border-b px-4">
                    <MobileSidebarTrigger />
                    <div className="border-l-4 border-q-orange pl-2 font-heading font-bold">
                        Asset Migration Console
                    </div>
                    <div className="ml-auto flex items-center gap-2">{headerActions}</div>
                </header>
                {selectedModule && jobsResult.status === "ok" ? (
                    <ModuleDetailSection
                        key={selectedModule.id}
                        module={selectedModule}
                        job={shownJob}
                        latestJob={latestJob}
                        setJobs={jobsResult.setData}
                        favorite={favorites.isFavorite(selectedModule.id)}
                        onShowLatestRun={selection.select}
                        onToggleFavorite={(module) => favorites.toggle(module.id)}
                    />
                ) : (
                    <div className="flex flex-1 justify-center items-center">
                        <EmptyCard icon={Cog} message="Select a module to execute it and see its logs" />
                    </div>
                )}
            </SidebarInset>
        </SidebarProvider>
    );
}

function MobileSidebarTrigger() {
    const { isMobile } = useSidebar();
    return isMobile ? <SidebarTrigger /> : null;
}

function clampSidebarWidth(width: number): number {
    return Math.min(Math.max(width, SIDEBAR_MIN_WIDTH), SIDEBAR_MAX_WIDTH);
}

function loadSidebarWidth(): number {
    try {
        const width = Number(localStorage.getItem(SIDEBAR_WIDTH_STORAGE_KEY));
        return width ? clampSidebarWidth(width) : SIDEBAR_DEFAULT_WIDTH;
    } catch {
        return SIDEBAR_DEFAULT_WIDTH;
    }
}

function saveSidebarWidth(width: number) {
    try {
        localStorage.setItem(SIDEBAR_WIDTH_STORAGE_KEY, String(width));
    } catch {
        // ignore, not a big deal
    }
}
