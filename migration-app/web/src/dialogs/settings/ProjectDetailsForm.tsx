import { type ProjectConfig, type SettingsFormProps } from "@/dialogs/settings/settingsTypes.ts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label.tsx";
import { fieldRow } from "@/dialogs/settings/formLayout.ts";
import { DraftTextField } from "@/dialogs/settings/DraftTextField.tsx";
import { Input } from "@/components/ui/input.tsx";

export function ProjectDetailsForm({
    settings,
    setSettings,
    onContextValidityChange,
}: SettingsFormProps & { onContextValidityChange: (valid: boolean) => void }) {
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
                <CardContent className="grid gap-4">
                    <div className={fieldRow}>
                        <Label>Target folder</Label>
                        <Input
                            value={settings.projectConfig.defaultTargetFolder ?? ""}
                            onChange={(e) => updateSettings("defaultTargetFolder", e.target.value || undefined)}
                        />
                    </div>
                    <div className={fieldRow}>
                        <Label>Variable structure ID</Label>
                        <Input
                            value={settings.projectConfig.defaultVariableStructure ?? ""}
                            onChange={(e) => updateSettings("defaultVariableStructure", e.target.value || undefined)}
                        />
                    </div>
                    <div className={fieldRow}>
                        <Label>Style definition path</Label>
                        <Input
                            value={settings.projectConfig.styleDefinitionPath ?? ""}
                            onChange={(e) => updateSettings("styleDefinitionPath", e.target.value || undefined)}
                        />
                    </div>
                    <div className={fieldRow}>
                        <Label>Language</Label>
                        <Input
                            value={settings.projectConfig.defaultLanguage ?? ""}
                            onChange={(e) => updateSettings("defaultLanguage", e.target.value || undefined)}
                        />
                    </div>
                </CardContent>
            </Card>
            <Card>
                <CardHeader>
                    <CardTitle>Paths</CardTitle>
                    <CardDescription>Override default paths in content manager</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-4">
                    <div className={fieldRow}>
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
                    <div className={fieldRow}>
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
                    <div className={fieldRow}>
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
                    <div className={fieldRow}>
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
                    <CardTitle>Partial migration</CardTitle>
                    <CardDescription>Migrate only a subset of the project</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-4">
                    <div className={fieldRow}>
                        <Label>Sub project id</Label>
                        <Input
                            value={settings.projectConfig.subProjectId ?? ""}
                            onChange={(e) =>
                                setSettings((prev) => ({
                                    ...prev,
                                    projectConfig: {
                                        ...prev.projectConfig,
                                        subProjectId: e.target.value || undefined,
                                    },
                                }))
                            }
                        />
                    </div>
                    <div className={fieldRow}>
                        <Label>Selected document objects file path</Label>
                        <Input
                            value={settings.projectConfig.selectedDocumentObjectsFile ?? ""}
                            onChange={(e) =>
                                setSettings((prev) => ({
                                    ...prev,
                                    projectConfig: {
                                        ...prev.projectConfig,
                                        selectedDocumentObjectsFile: e.target.value || undefined,
                                    },
                                }))
                            }
                        />
                    </div>
                    <div className={fieldRow}>
                        <Label>Selected document objects</Label>
                        <DraftTextField
                            value={settings.projectConfig.selectedDocumentObjects}
                            format={(list) => list?.join(", ") ?? ""}
                            parse={parseList}
                            onCommit={(list) =>
                                setSettings((prev) => ({
                                    ...prev,
                                    projectConfig: { ...prev.projectConfig, selectedDocumentObjects: list },
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
                    <DraftTextField
                        multiline
                        value={settings.projectConfig.context}
                        format={(context) => JSON.stringify(context, null, 2)}
                        parse={parseContext}
                        onValidityChange={onContextValidityChange}
                        onCommit={(context) =>
                            setSettings((prev) => ({ ...prev, projectConfig: { ...prev.projectConfig, context } }))
                        }
                    />
                </CardContent>
            </Card>
        </div>
    );
}

function parseList(text: string): string[] | undefined {
    const items = text
        .split(",")
        .map((item) => item.trim())
        .filter((item) => item.length > 0);
    return items.length > 0 ? items : undefined;
}

function parseContext(text: string): Record<string, unknown> {
    const parsed: unknown = text.trim() === "" ? {} : JSON.parse(text);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
        throw new Error("Context must be a JSON object");
    }
    return parsed as Record<string, unknown>;
}
