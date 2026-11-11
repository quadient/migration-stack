import { Settings as SettingsIcon } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { TooltipProvider } from "@/components/ui/tooltip.tsx";
import { ThemeToggle } from "@/common/ThemeToggle.tsx";
import { ViewToggle } from "@/common/ViewToggle.tsx";
import SettingsDialog from "@/dialogs/settings/SettingsDialog.tsx";
import type { Settings } from "@/dialogs/settings/settingsTypes.ts";
import InputRequestDialog from "@/dialogs/input/InputRequestDialog.tsx";
import { useFetch } from "@/hooks/useFetch.ts";
import { useView } from "@/hooks/useView.ts";
import DashboardView from "@/views/DashboardView.tsx";
import RunnerView from "@/views/RunnerView.tsx";
import { getSourceFormats, type ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job } from "@/types/job.ts";

export default function App() {
    const [view, setView] = useView();
    const modulesResult = useFetch<ModuleMetadata[]>("/api/scripts");
    const jobsResult = useFetch<Job[]>("/api/job/list");
    const settingsResult = useFetch<Settings>("/api/settings");

    const headerActions = (
        <>
            <ThemeToggle />
            <ViewToggle view={view} onViewChange={setView} />
            {settingsResult.status === "ok" && (
                <SettingsDialog
                    loadedSettings={settingsResult.data}
                    setLoadedSettings={settingsResult.setData}
                    onSaved={jobsResult.refetch}
                    trigger={
                        <Button variant="outline">
                            <SettingsIcon className="w-4 h-4 mr-2" />
                            Settings
                        </Button>
                    }
                    sourceFormats={getSourceFormats(modulesResult)}
                />
            )}
        </>
    );

    const View = view === "runner" ? RunnerView : DashboardView;

    return (
        <TooltipProvider>
            <InputRequestDialog />
            <View
                modulesResult={modulesResult}
                jobsResult={jobsResult}
                settingsResult={settingsResult}
                headerActions={headerActions}
            />
        </TooltipProvider>
    );
}
