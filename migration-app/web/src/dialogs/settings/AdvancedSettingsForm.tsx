import { type ProjectConfig, type SettingsFormProps } from "@/dialogs/settings/settingsTypes.ts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";

export function AdvancedSettingsForm({ settings, setSettings }: SettingsFormProps) {
    const updateSettings = (key: keyof ProjectConfig, value: string) => {
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
                        <Label>Target Folder</Label>
                        <Input
                            value={settings.projectConfig.defaultTargetFolder ?? ""}
                            onChange={(e) => updateSettings("defaultTargetFolder", e.target.value)}
                        />
                    </div>
                    <div className="grid gap-3">
                        <div className="grid gap-3">
                            <Label>Variable Structure ID</Label>
                            <Input
                                value={settings.projectConfig.defaultVariableStructure ?? ""}
                                onChange={(e) => updateSettings("defaultVariableStructure", e.target.value)}
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
                <CardContent>
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
                                            images: e.target.value,
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
