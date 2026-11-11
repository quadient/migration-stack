import type { ReactNode } from "react";
import { Separator } from "@/components/ui/separator.tsx";
import ChartsSection from "@/sections/chartsSection/ChartsSection.tsx";
import ModulesSection from "@/sections/modulesSection/ModulesSection.tsx";
import type { UseFetchResult } from "@/hooks/useFetch.ts";
import type { Settings } from "@/dialogs/settings/settingsTypes.ts";
import type { ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job } from "@/types/job.ts";

type DashboardViewProps = {
    modulesResult: UseFetchResult<ModuleMetadata[]>;
    jobsResult: UseFetchResult<Job[]>;
    settingsResult: UseFetchResult<Settings>;
    headerActions: ReactNode;
};

export default function DashboardView({
    modulesResult,
    jobsResult,
    settingsResult,
    headerActions,
}: DashboardViewProps) {
    return (
        <div className="grid grid-rows-[auto_1fr] min-h-screen max-h-screen px-[15%]">
            <div>
                <header className="pt-8">
                    <div className="flex justify-between items-center">
                        <div className="text-xl font-bold tracking-tight">Asset Migration Console</div>
                        <div className="flex items-center gap-2">{headerActions}</div>
                    </div>
                    <div className="text-muted-foreground">Trigger processes for asset migration and deployment</div>
                </header>
                <Separator className="my-6" />
            </div>
            {modulesResult.status === "ok" && settingsResult.status === "ok" && jobsResult.status === "ok" && (
                <main className="flex flex-1 justify-center min-h-0 gap-4">
                    <ChartsSection jobs={jobsResult.data} projectName={settingsResult.data.projectConfig.name} />
                    <ModulesSection
                        modules={modulesResult.data}
                        sourceFormat={settingsResult.data.sourceFormat}
                        jobsResult={jobsResult}
                    />
                </main>
            )}
        </div>
    );
}
