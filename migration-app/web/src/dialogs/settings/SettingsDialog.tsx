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
import { ProjectDetailsForm } from "@/dialogs/settings/ProjectDetailsForm.tsx";
import { Upload } from "lucide-react";
import { errorMessage, fetchSettings } from "@/common/configApi.ts";

type SettingsDialogProps = {
    trigger: ReactNode;
    loadedSettings: Settings;
    setLoadedSettings: (value: ((prev: Settings) => Settings) | Settings) => void;
    onSaved: () => void;
};

export default function SettingsDialog({ trigger, loadedSettings, setLoadedSettings, onSaved }: SettingsDialogProps) {
    const [open, setOpen] = useState(false);
    const [settings, setSettings] = useState(loadedSettings);
    const [saveError, setSaveError] = useState<string | null>(null);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [importResult, setImportResult] = useState<ImportResult | null>(null);
    const [contextValid, setContextValid] = useState(true);

    // Reloads the config files, so Save does not revert edits made to them since the page loaded.
    // Without fresh settings, Save is disabled so it cannot overwrite the files with stale values.
    const openWithFreshSettings = async () => {
        let fresh = loadedSettings;
        let error: string | null = null;
        try {
            fresh = await fetchSettings();
            setLoadedSettings(fresh);
        } catch (e) {
            error = `Could not reload settings, Save is disabled: ${errorMessage(e)}`;
        }
        setSettings(fresh);
        setLoadError(error);
        setSaveError(null);
        setImportResult(null);
        setContextValid(true);
        setOpen(true);
    };

    return (
        <Dialog modal open={open} onOpenChange={(isOpen) => (isOpen ? openWithFreshSettings() : setOpen(false))}>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent className="sm:max-w-5xl">
                <DialogHeader>
                    <DialogTitle>Settings</DialogTitle>
                    <DialogDescription className="text-muted-foreground">
                        Project config and migration config are separate files, saved together
                    </DialogDescription>
                </DialogHeader>
                <Tabs defaultValue="project">
                    <TabsList>
                        <TabsTrigger value="project">Project config</TabsTrigger>
                        <TabsTrigger value="migration">Migration config</TabsTrigger>
                    </TabsList>
                    <TabsContent value="project" forceMount className="data-[state=inactive]:hidden">
                        <ConfigFileName name={`${settings.activeProjectConfig}-project-config.toml`} />
                        <ScrollArea className="h-[60vh] pr-4">
                            <div className="grid gap-6">
                                <ProjectSettingsForm settings={settings} setSettings={setSettings} />
                                <ProjectDetailsForm
                                    settings={settings}
                                    setSettings={setSettings}
                                    onContextValidityChange={setContextValid}
                                />
                            </div>
                        </ScrollArea>
                    </TabsContent>
                    <TabsContent value="migration" forceMount className="data-[state=inactive]:hidden">
                        <ConfigFileName name={`${settings.activeMigrationConfig}-migration-config.toml`} />
                        <ScrollArea className="h-[60vh] pr-4">
                            <ConnectionSettingsForm settings={settings} setSettings={setSettings} />
                        </ScrollArea>
                    </TabsContent>
                </Tabs>
                {importResult && (
                    <p className={`mt-4 text-sm ${importResult.ok ? "text-muted-foreground" : "text-destructive"}`}>
                        {importResult.message}
                    </p>
                )}
                {loadError && <p className="mt-4 text-sm text-destructive">{loadError}</p>}
                {saveError && <p className="mt-4 text-sm text-destructive">{saveError}</p>}
                {!contextValid && (
                    <p className="mt-4 text-sm text-destructive">
                        Context is not valid JSON (Project config → Context). Fix it to save.
                    </p>
                )}
                <DialogFooter className="flex mt-6 sm:justify-between">
                    <FileUploadButton
                        settings={settings}
                        setSettings={setSettings}
                        onResult={(result) => {
                            setImportResult(result);
                            if (result.ok) setContextValid(true);
                        }}
                    />
                    <div className="flex gap-4">
                        <DialogClose asChild>
                            <Button variant="outline">Cancel</Button>
                        </DialogClose>
                        <Button
                            disabled={!contextValid || loadError !== null}
                            onClick={async () => {
                                try {
                                    await onSaveChanges(settings, setLoadedSettings, onSaved);
                                    setOpen(false);
                                } catch (e) {
                                    setSaveError(errorMessage(e));
                                }
                            }}
                        >
                            Save Changes
                        </Button>
                    </div>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

type ImportResult = { ok: boolean; message: string };

function ConfigFileName({ name }: { name: string }) {
    return <p className="mb-3 font-mono text-xs text-muted-foreground">{name}</p>;
}

function FileUploadButton({
    settings,
    setSettings,
    onResult,
}: {
    settings: Settings;
    setSettings: (value: ((prev: Settings) => Settings) | Settings) => void;
    onResult: (result: ImportResult) => void;
}) {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        e.target.value = "";
        if (!file) return;

        if (!file.name.endsWith(".toml")) {
            onResult({ ok: false, message: `Import failed: '${file.name}' is not a .toml file` });
            return;
        }

        try {
            const fileContent = await file.text();
            const parsedFileContent = parse(fileContent);
            const configType = detectConfigType(parsedFileContent, settings);
            if (!configType) {
                onResult({ ok: false, message: `Import failed: '${file.name}' has no known settings fields` });
                return;
            }

            const wrappedFileContent = { [configType]: parsedFileContent } as Partial<Settings>;

            setSettings((prev) => deepMerge(prev, wrappedFileContent));
            const target = configType === "projectConfig" ? "project config" : "migration config";
            onResult({ ok: true, message: `Imported '${file.name}' into the ${target}. Save to write it.` });
        } catch (err) {
            onResult({ ok: false, message: `Import failed: ${errorMessage(err)}` });
        }
    };

    return (
        <div className="flex items-center gap-2">
            <Button variant="secondary" onClick={() => fileInputRef.current?.click()}>
                <Upload />
                Import Settings
            </Button>
            <input type="file" accept=".toml" ref={fileInputRef} onChange={handleFileChange} className="hidden" />
        </div>
    );
}

async function onSaveChanges(
    settings: Settings | null,
    setLoadedSettings: (value: ((prev: Settings) => Settings) | Settings) => void,
    onSaved: () => void,
): Promise<void> {
    if (!settings) return;

    const response = await fetch("/api/settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(settings),
    });
    if (!response.ok) {
        throw new Error((await response.text()) || "Failed to save settings");
    } else {
        setLoadedSettings(settings);
        onSaved();
    }
}

function detectConfigType(uploadedSettings: Record<string, unknown>, currentSettings: Settings): keyof Settings | null {
    const uploadedSettingsKeys = Object.keys(uploadedSettings);
    const currentProjectKeys = Object.keys(currentSettings.projectConfig);
    const currentMigrationKeys = Object.keys(currentSettings.migrationConfig);

    for (const key of uploadedSettingsKeys) {
        if (currentProjectKeys.includes(key)) return "projectConfig";
        if (currentMigrationKeys.includes(key)) return "migrationConfig";
    }

    return null;
}

function deepMerge<T extends Record<string, unknown>>(target: T, source: Partial<T>): T {
    const result: T = { ...target };

    for (const key in source) {
        const sourceValue = source[key];
        const targetValue = target[key];

        if (sourceValue === undefined || sourceValue === null) {
            continue;
        }

        if (
            typeof sourceValue === "object" &&
            !Array.isArray(sourceValue) &&
            typeof targetValue === "object" &&
            targetValue !== null &&
            targetValue !== undefined
        ) {
            result[key] = deepMerge(
                targetValue as Record<string, unknown>,
                sourceValue as Record<string, unknown>,
            ) as T[Extract<keyof T, string>];
        } else {
            result[key] = sourceValue as T[Extract<keyof T, string>];
        }
    }

    return result;
}
