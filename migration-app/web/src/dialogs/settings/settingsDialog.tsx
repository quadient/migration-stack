import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog.tsx";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { type Settings } from "@/dialogs/settings/settingsTypes.tsx";
import { Button } from "@/components/ui/button.tsx";
import { ProjectSettingsForm } from "@/dialogs/settings/projectSettingsForm.tsx";
import { MigrationSettingsForm } from "@/dialogs/settings/migrationSettingsForm.tsx";
// import { useFetch } from "@/hooks/useFetch.ts";

type SettingsDialogProps = {
    trigger: ReactNode;
};

export default function SettingsDialog({ trigger }: SettingsDialogProps) {
    // const settingsResult = useFetch<Settings | null>("/api/settings", null);

    const [settings, setSettings] = useState<Settings | null>(null);
    const [open, setOpen] = useState(false);

    console.log(settings);

    useEffect(() => {
        fetchSettings()
            .then((json) => setSettings(json))
            .catch(() => {
                console.error("Failed to fetch or parse settings");
                setSettings(null);
            });
    }, []);

    return (
        <Dialog modal open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent className="min-w-2/3">
                <DialogHeader>
                    <DialogTitle>Settings</DialogTitle>
                    <DialogDescription className="text-gray-500">
                        Configure project settings and connection to services
                    </DialogDescription>
                </DialogHeader>
                {settings && (
                    <>
                        <Tabs defaultValue="project">
                            <TabsList>
                                <TabsTrigger value="project">Project Settings</TabsTrigger>
                                <TabsTrigger value="connections">Connections</TabsTrigger>
                                <TabsTrigger value="advanced">Advanced</TabsTrigger>
                            </TabsList>
                            <TabsContent value="project">{/* Project Settings form fields */}</TabsContent>
                            <TabsContent value="connections">{/* Connections form fields */}</TabsContent>
                            <TabsContent value="advanced">{/* Advanced form fields */}</TabsContent>
                        </Tabs>
                        <div className="flex flex-row max-h-120">
                            <div className="flex-1 overflow-y-auto pr-4 border-r">
                                <ProjectSettingsForm settings={settings} setSettings={setSettings} />
                            </div>
                            <div className="flex-1 overflow-y-auto pl-4 pr-4">
                                <MigrationSettingsForm settings={settings} setSettings={setSettings} />
                            </div>
                        </div>
                    </>
                )}
                <DialogFooter>
                    <Button
                        type={"submit"}
                        onClick={() => {
                            setOpen(false);
                            return onSaveChanges(settings);
                        }}
                    >
                        Save Changes
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

async function onSaveChanges(settings: Settings | null): Promise<void> {
    if (!settings) return;

    const response = await fetch("/api/settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(settings),
    });
    if (!response.ok) {
        throw new Error("Failed to save settings");
    }
}

async function fetchSettings(): Promise<Settings> {
    const response = await fetch("/api/settings");
    if (!response.ok) {
        throw new Error("Failed to fetch settings");
    }
    return await response.json();
}
