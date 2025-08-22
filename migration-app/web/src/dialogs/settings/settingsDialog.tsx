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
import { ScrollArea } from "@/components/ui/scroll-area";
import { type Settings } from "@/dialogs/settings/settingsTypes.tsx";
import { Button } from "@/components/ui/button.tsx";
import { ProjectSettingsForm } from "@/dialogs/settings/projectSettingsForm.tsx";
import { ConnectionSettingsForm } from "@/dialogs/settings/connectionSettingsForm.tsx";
import { AdvancedSettingsForm } from "@/dialogs/settings/advancedSettingsForm.tsx";
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
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Settings</DialogTitle>
                    <DialogDescription className="text-gray-500">
                        Configure project and connection settings
                    </DialogDescription>
                </DialogHeader>
                {settings && (
                    <Tabs defaultValue="project">
                        <TabsList>
                            <TabsTrigger value="project">Project</TabsTrigger>
                            <TabsTrigger value="connections">Connections</TabsTrigger>
                            <TabsTrigger value="advanced">Advanced</TabsTrigger>
                        </TabsList>
                        <ScrollArea className="h-[500px] pr-4">
                            <TabsContent value="project">
                                <ProjectSettingsForm settings={settings} setSettings={setSettings} />
                            </TabsContent>
                            <TabsContent value="connections">
                                <ConnectionSettingsForm settings={settings} setSettings={setSettings} />
                            </TabsContent>
                            <TabsContent value="advanced">
                                <AdvancedSettingsForm settings={settings} setSettings={setSettings} />
                            </TabsContent>
                        </ScrollArea>
                    </Tabs>
                )}
                <DialogFooter className="mt-6">
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
