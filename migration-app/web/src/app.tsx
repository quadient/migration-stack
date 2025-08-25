import { Button } from "@/components/ui/button";
import { Settings as SettingsIcon } from "lucide-react";
import SettingsDialog from "./dialogs/settings/settingsDialog.tsx";
import { useFetch } from "./hooks/useFetch.ts";
import { Separator } from "@/components/ui/separator.tsx";
import type { Settings } from "@/dialogs/settings/settingsTypes.tsx";
import ModulesSection from "@/sections/modulesSection/ModulesSection.tsx";
import ChartsSection from "@/sections/chartsSection/ChartsSection.tsx";

export default function App() {
    const settingsResult = useFetch<Settings>("/api/settings");

    return (
        <div className="min-h-screen grid grid-rows-[auto_1fr] px-[15%]">
            <div>
                <header className="pt-8">
                    <div className="flex justify-between items-center">
                        <div className="text-xl font-bold tracking-tight">Asset Migration Console</div>
                        <SettingsDialog
                            settingsResult={settingsResult}
                            trigger={
                                <Button variant="outline">
                                    <SettingsIcon className="w-4 h-4 mr-2" />
                                    Settings
                                </Button>
                            }
                        />
                    </div>
                    <div className="text-muted-foreground">Trigger processes for asset migration and deployment</div>
                </header>
                <Separator className="my-6" />
            </div>
            <main className="flex flex-1 min-h-0 gap-4">
                <ChartsSection />
                <ModulesSection />
            </main>
        </div>
    );
}
