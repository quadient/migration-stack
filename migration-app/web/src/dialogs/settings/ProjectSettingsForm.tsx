import {
    type InspireOutput,
    inspireOutputOptions,
    type ProjectConfig,
    type Settings,
    type SettingsFormProps,
} from "@/dialogs/settings/settingsTypes.ts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";
import { SettingsCombobox } from "@/dialogs/settings/SettingsCombobox.tsx";

export function ProjectSettingsForm({
    settings,
    setSettings,
    sourceFormats,
}: SettingsFormProps & { sourceFormats?: string[] }) {
    const updateSettings = (key: keyof ProjectConfig, value: string | undefined) => {
        setSettings((prev) => ({ ...prev, projectConfig: { ...prev.projectConfig, [key]: value } }));
    };

    return (
        <div className="grid gap-6">
            <Card>
                <CardHeader>
                    <CardTitle>General</CardTitle>
                    <CardDescription>Basic project configuration</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-6">
                    <div className="grid gap-3">
                        <Label>Name</Label>
                        <Input
                            value={settings.projectConfig.name}
                            onChange={(e) => updateSettings("name", e.target.value)}
                        />
                    </div>
                    <div className="grid gap-3">
                        <Label>Input data path</Label>
                        <Input
                            value={settings.projectConfig.inputDataPath}
                            onChange={(e) => updateSettings("inputDataPath", e.target.value)}
                        />
                    </div>
                    <div className="grid gap-3">
                        <Label>Source format</Label>
                        <SettingsCombobox
                            currentValue={settings.sourceFormat}
                            onChange={(newValue) => {
                                setSettings((prev: Settings) => ({
                                    ...prev,
                                    sourceFormat: newValue,
                                }));
                            }}
                            options={sourceFormats}
                        />
                    </div>
                    <div className="grid gap-3">
                        <Label>Inspire output</Label>
                        <SettingsCombobox
                            currentValue={settings.projectConfig.inspireOutput}
                            onChange={(newValue) => {
                                setSettings((prev: Settings) => ({
                                    ...prev,
                                    projectConfig: {
                                        ...prev.projectConfig,
                                        inspireOutput: newValue as InspireOutput,
                                    },
                                }));
                            }}
                            options={[...inspireOutputOptions]}
                        />
                    </div>
                    <div className="grid gap-3">
                        <Label>Source base template path</Label>
                        <Input
                            value={settings.projectConfig.sourceBaseTemplatePath ?? ""}
                            onChange={(e) => updateSettings("sourceBaseTemplatePath", e.target.value || undefined)}
                        />
                    </div>
                </CardContent>
            </Card>
            {settings.projectConfig.inspireOutput !== "Designer" && (
                <Card>
                    <CardHeader>
                        <CardTitle>{`${settings.projectConfig.inspireOutput} Output`}</CardTitle>
                        <CardDescription>{`Inspire ${settings.projectConfig.inspireOutput} specific configuration`}</CardDescription>
                    </CardHeader>
                    <CardContent className="grid gap-6">
                        <div className="grid gap-3">
                            <Label>Tenant</Label>
                            <Input
                                value={settings.projectConfig.interactiveTenant}
                                onChange={(e) => updateSettings("interactiveTenant", e.target.value)}
                            />
                        </div>
                        <div className="grid gap-3">
                            <Label>Base template path</Label>
                            <Input
                                value={settings.projectConfig.baseTemplatePath}
                                onChange={(e) => updateSettings("baseTemplatePath", e.target.value)}
                            />
                        </div>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}
