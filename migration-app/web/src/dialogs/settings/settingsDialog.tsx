import type { ReactNode } from "react";
import { useState } from "react";
import {
    Dialog,
    DialogClose,
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

import type { UseFetchResult } from "@/hooks/useFetch.ts";

type SettingsDialogProps = {
    trigger: ReactNode;
    settingsResult: UseFetchResult<Settings>;
    sourceFormats: string[] | undefined;
};

export default function SettingsDialog({ trigger, settingsResult, sourceFormats }: SettingsDialogProps) {
    const [open, setOpen] = useState(false);

    return (
        <Dialog modal open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Settings</DialogTitle>
                    <DialogDescription className="text-muted-foreground">
                        Configure project and connection settings
                    </DialogDescription>
                </DialogHeader>
                {settingsResult.status === "ok" && settingsResult.data && (
                    <>
                        <Tabs defaultValue="project">
                            <TabsList>
                                <TabsTrigger value="project">Project</TabsTrigger>
                                <TabsTrigger value="connections">Connections</TabsTrigger>
                                <TabsTrigger value="advanced">Advanced</TabsTrigger>
                            </TabsList>
                            <ScrollArea className="h-[500px] pr-4">
                                <TabsContent value="project">
                                    <ProjectSettingsForm
                                        settings={settingsResult.data}
                                        setSettings={settingsResult.setData}
                                        sourceFormats={sourceFormats}
                                    />
                                </TabsContent>
                                <TabsContent value="connections">
                                    <ConnectionSettingsForm
                                        settings={settingsResult.data}
                                        setSettings={settingsResult.setData}
                                    />
                                </TabsContent>
                                <TabsContent value="advanced">
                                    <AdvancedSettingsForm
                                        settings={settingsResult.data}
                                        setSettings={settingsResult.setData}
                                    />
                                </TabsContent>
                            </ScrollArea>
                        </Tabs>
                        <DialogFooter className="mt-6">
                            <DialogClose asChild>
                                <Button variant="outline">Cancel</Button>
                            </DialogClose>
                            <Button
                                type={"submit"}
                                onClick={() => {
                                    setOpen(false);
                                    return onSaveChanges(settingsResult.data);
                                }}
                            >
                                Save Changes
                            </Button>
                        </DialogFooter>
                    </>
                )}
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
