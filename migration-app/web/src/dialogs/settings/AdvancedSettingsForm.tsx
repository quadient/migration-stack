import { type ProjectConfig, type SettingsFormProps } from "@/dialogs/settings/settingsTypes.ts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";

export function AdvancedSettingsForm({ settings, setSettings }: SettingsFormProps) {
    const updateSettings = (key: keyof ProjectConfig, value: string | undefined) => {
        setSettings((prev) => ({ ...prev, projectConfig: { ...prev.projectConfig, [key]: value } }));
    };

    return (
        <div className="grid gap-6">
            <Card>
                <CardHeader>
                    <CardTitle>Default Values</CardTitle>
                    <CardDescription>Configure default values</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-6">
                    <div className="grid gap-3">
                        <Label>Target folder</Label>
                        <Input
                            value={settings.projectConfig.defaultTargetFolder ?? ""}
                            onChange={(e) => updateSettings("defaultTargetFolder", e.target.value || undefined)}
                        />
                    </div>
                    <div className="grid gap-3">
                        <div className="grid gap-3">
                            <Label>Variable structure ID</Label>
                            <Input
                                value={settings.projectConfig.defaultVariableStructure ?? ""}
                                onChange={(e) =>
                                    updateSettings("defaultVariableStructure", e.target.value || undefined)
                                }
                            />
                        </div>
                    </div>
                    <div className="grid gap-3">
                        <div className="grid gap-3">
                            <Label>Style definition path</Label>
                            <Input
                                value={settings.projectConfig.styleDefinitionPath ?? ""}
                                onChange={(e) => updateSettings("styleDefinitionPath", e.target.value || undefined)}
                            />
                        </div>
                    </div>
                    <div className="grid gap-3">
                        <div className="grid gap-3">
                            <Label>Language</Label>
                            <Input
                                value={settings.projectConfig.defaultLanguage ?? ""}
                                onChange={(e) => updateSettings("defaultLanguage", e.target.value || undefined)}
                            />
                        </div>
                    </div>
                </CardContent>
            </Card>
            <Card>
                <CardHeader>
                    <CardTitle>Paths</CardTitle>
                    <CardDescription>Override default paths in content manager</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-6">
                    <div className="grid gap-3">
                        <Label>Images</Label>
                        <Input
                            value={settings.projectConfig.paths.images ?? ""}
                            onChange={(e) =>
                                setSettings((prev) => ({
                                    ...prev,
                                    projectConfig: {
                                        ...prev.projectConfig,
                                        paths: {
                                            ...prev.projectConfig.paths,
                                            images: e.target.value || undefined,
                                        },
                                    },
                                }))
                            }
                        />
                    </div>
                    <div className="grid gap-3">
                        <Label>Fonts</Label>
                        <Input
                            value={settings.projectConfig.paths.fonts ?? ""}
                            onChange={(e) =>
                                setSettings((prev) => ({
                                    ...prev,
                                    projectConfig: {
                                        ...prev.projectConfig,
                                        paths: {
                                            ...prev.projectConfig.paths,
                                            fonts: e.target.value || undefined,
                                        },
                                    },
                                }))
                            }
                        />
                    </div>
                    <div className="grid gap-3">
                        <Label>Documents</Label>
                        <Input
                            value={settings.projectConfig.paths.documents ?? ""}
                            onChange={(e) =>
                                setSettings((prev) => ({
                                    ...prev,
                                    projectConfig: {
                                        ...prev.projectConfig,
                                        paths: {
                                            ...prev.projectConfig.paths,
                                            documents: e.target.value || undefined,
                                        },
                                    },
                                }))
                            }
                        />
                    </div>
                    <div className="grid gap-3">
                        <Label>Attachments</Label>
                        <Input
                            value={settings.projectConfig.paths.attachments ?? ""}
                            onChange={(e) =>
                                setSettings((prev) => ({
                                    ...prev,
                                    projectConfig: {
                                        ...prev.projectConfig,
                                        paths: {
                                            ...prev.projectConfig.paths,
                                            attachments: e.target.value || undefined,
                                        },
                                    },
                                }))
                            }
                        />
                    </div>
                </CardContent>
            </Card>
            <Card>
                <CardHeader>
                    <CardTitle>Context</CardTitle>
                    <CardDescription>Custom values map for parse modules</CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="grid gap-3">
                        <Input
                            value={JSON.stringify(settings.projectConfig.context)}
                            onChange={(e) => {
                                let value = e.target.value;
                                let parsed: Record<string, any> = {};
                                try {
                                    parsed = (value.trim() === "" ? {} : JSON.parse(value)) as Record<string, any>;
                                    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
                                        console.warn("Invalid context value", value);
                                        return;
                                    }
                                    setSettings((prev) => ({
                                        ...prev,
                                        projectConfig: {
                                            ...prev.projectConfig,
                                            context: parsed,
                                        },
                                    }));
                                } catch (err) {
                                    console.warn("Invalid context value", value, err);
                                }
                            }}
                        />
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
