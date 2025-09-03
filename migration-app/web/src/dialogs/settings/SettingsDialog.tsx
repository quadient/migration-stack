import React, { type ReactNode, useRef } from "react";
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
import { type Settings } from "@/dialogs/settings/settingsTypes.ts";
import { Button } from "@/components/ui/button.tsx";
import { parse } from "toml";
import { ProjectSettingsForm } from "@/dialogs/settings/ProjectSettingsForm.tsx";
import { ConnectionSettingsForm } from "@/dialogs/settings/ConnectionSettingsForm.tsx";
import { AdvancedSettingsForm } from "@/dialogs/settings/AdvancedSettingsForm.tsx";

type SettingsDialogProps = {
    trigger: ReactNode;
    sourceFormats: string[] | undefined;
    loadedSettings: Settings;
    setLoadedSettings: (value: ((prev: Settings) => Settings) | Settings) => void;
};

export default function SettingsDialog({
    trigger,
    sourceFormats,
    loadedSettings,
    setLoadedSettings,
}: SettingsDialogProps) {
    const [open, setOpen] = useState(false);
    const [settings, setSettings] = useState(loadedSettings);

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
                                    settings={settings}
                                    setSettings={setSettings}
                                    sourceFormats={sourceFormats}
                                />
                            </TabsContent>
                            <TabsContent value="connections">
                                <ConnectionSettingsForm settings={settings} setSettings={setSettings} />
                            </TabsContent>
                            <TabsContent value="advanced">
                                <AdvancedSettingsForm settings={settings} setSettings={setSettings} />
                            </TabsContent>
                        </ScrollArea>
                    </Tabs>
                    <DialogFooter className="flex mt-6 sm:justify-between">
                        <FileUploadButton />
                        <div className="flex gap-4">
                            <DialogClose asChild>
                                <Button variant="outline">Cancel</Button>
                            </DialogClose>
                            <Button
                                type={"submit"}
                                onClick={() => {
                                    setOpen(false);
                                    return onSaveChanges(settings, setLoadedSettings);
                                }}
                            >
                                Save Changes
                            </Button>
                        </div>
                    </DialogFooter>
                </>
            </DialogContent>
        </Dialog>
    );
}

function FileUploadButton() {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        if (!file.name.endsWith(".toml")) {
            console.error(`File '${file.name}' is not a .toml file.`);
            return;
        }

        try {
            const fileContent = await file.text();
            console.log("file.text():", fileContent);
            const parsed = parse(fileContent); // Deserialize TOML → JS object
            console.log("parse():", parsed);
            // setConfig(parsed);
            // console.log("Parsed TOML config:", parsed);
        } catch (err) {
            console.error("Error parsing TOML:", err);
            alert("Failed to parse TOML file");
        }
    };

    return (
        <div className="flex items-center gap-2">
            <Button variant="default" onClick={() => fileInputRef.current?.click()}>
                Upload File
            </Button>
            <input type="file" accept=".toml" ref={fileInputRef} onChange={handleFileChange} className="hidden" />
        </div>
    );
}

async function onSaveChanges(
    settings: Settings | null,
    setLoadedSettings: (value: ((prev: Settings) => Settings) | Settings) => void,
): Promise<void> {
    if (!settings) return;

    const response = await fetch("/api/settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(settings),
    });
    if (!response.ok) {
        throw new Error("Failed to save settings");
    } else {
        setLoadedSettings(settings);
    }
}
