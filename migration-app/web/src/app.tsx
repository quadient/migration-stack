import { Button } from "@/components/ui/button";
import { Settings as SettingsIcon } from "lucide-react";
import SettingsDialog from "./dialogs/settings/SettingsDialog.tsx";
import { Separator } from "@/components/ui/separator.tsx";
import type { Settings } from "@/dialogs/settings/settingsTypes.ts";
import ModulesSection from "@/sections/modulesSection/ModulesSection.tsx";
import ChartsSection, { type TypeStatistics } from "@/sections/chartsSection/ChartsSection.tsx";
import { useMemo } from "react";
import { useFetch, type UseFetchResult } from "@/hooks/useFetch.ts";
import type { ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job } from "@/types/job.ts";

export default function App() {
    const modulesResult = useFetch<ModuleMetadata[]>("/api/scripts");
    const settingsResult = useFetch<Settings>("/api/settings");
    const jobsResult = useFetch<Job[]>("/api/job/list");

    const settingsMemo = useMemo(
        () => (settingsResult.status === "ok" ? settingsResult.data.projectConfig.name : undefined),
        [settingsResult],
    );

    const jobsMemo = useMemo(
        () => JSON.stringify((jobsResult.status === "ok" ? jobsResult.data : []).map((it) => it.lastUpdated)),
        [jobsResult],
    );

    const statisticsResult = useFetch<TypeStatistics>("/api/statistics", undefined, [jobsMemo, settingsMemo]);

    const sourceFormats = getSourceFormats(modulesResult);

    return (
        <div className="grid grid-rows-[auto_1fr] min-h-screen max-h-screen px-[15%]">
            <div>
                <header className="pt-8">
                    <div className="flex justify-between items-center">
                        <div className="text-xl font-bold tracking-tight">Asset Migration Console</div>
                        {settingsResult.status === "ok" && (
                            <SettingsDialog
                                loadedSettings={settingsResult.data}
                                setLoadedSettings={settingsResult.setData}
                                trigger={
                                    <Button variant="outline">
                                        <SettingsIcon className="w-4 h-4 mr-2" />
                                        Settings
                                    </Button>
                                }
                                sourceFormats={sourceFormats}
                            />
                        )}
                    </div>
                    <div className="text-muted-foreground">Trigger processes for asset migration and deployment</div>
                </header>
                <Separator className="my-6" />
            </div>
            <main className="flex flex-1 min-h-0 gap-4">
                <ChartsSection statisticsResult={statisticsResult} />
                {modulesResult.status === "ok" && settingsResult.status === "ok" && jobsResult.status === "ok" && (
                    <ModulesSection
                        modules={modulesResult.data}
                        sourceFormat={settingsResult.data.sourceFormat}
                        jobsResult={jobsResult}
                    />
                )}
            </main>
        </div>
    );
}

function getSourceFormats(scriptsResult: UseFetchResult<ModuleMetadata[]>): string[] | undefined {
    return scriptsResult.status === "ok"
        ? ([...new Set(scriptsResult.data.map((script) => script.sourceFormat).filter(Boolean))] as string[])
        : undefined;
}
