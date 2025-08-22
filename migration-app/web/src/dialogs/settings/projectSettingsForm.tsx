import * as React from "react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";
import {
    type InspireOutput,
    inspireOutputOptions,
    type ProjectConfig,
    type Settings,
    type SettingsFormProps,
} from "@/dialogs/settings/settingsTypes.tsx";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils.ts";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";

export function ProjectSettingsForm({ settings, setSettings }: SettingsFormProps) {
    const updateSettings = (key: keyof ProjectConfig, value: string) => {
        setSettings((prev) => (prev ? { ...prev, projectConfig: { ...prev.projectConfig, [key]: value } } : null));
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
                        <Label>Inspire output</Label>
                        <InspireOutputCombobox
                            currentValue={settings.projectConfig.inspireOutput}
                            setSettings={setSettings}
                        />
                    </div>
                </CardContent>
            </Card>
            <Card>
                <CardHeader>
                    <CardTitle>{`${settings.projectConfig.inspireOutput} Output`}</CardTitle>
                    <CardDescription>{`Inspire ${settings.projectConfig.inspireOutput} specific configuration`}</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-6">
                    {settings.projectConfig.inspireOutput !== "Designer" && (
                        <>
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
                        </>
                    )}
                    {settings.projectConfig.inspireOutput === "Designer" && (
                        <div className="grid gap-3">
                            <Label>Source Base template path</Label>
                            <Input
                                value={settings.projectConfig.sourceBaseTemplatePath ?? ""}
                                onChange={(e) => updateSettings("sourceBaseTemplatePath", e.target.value)}
                            />
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}

type InspireOutputComboboxProps = {
    currentValue: InspireOutput;
    setSettings: (value: ((prevState: Settings | null) => Settings | null) | Settings | null) => void;
};

function InspireOutputCombobox({ currentValue, setSettings }: InspireOutputComboboxProps) {
    const [open, setOpen] = React.useState(false);

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button variant="outline" role="combobox" aria-expanded={open} className="justify-between font-normal">
                    {currentValue}
                    <ChevronsUpDown className="opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="p-0 w-100">
                <Command>
                    <CommandList>
                        <CommandGroup>
                            {inspireOutputOptions.map((outputOption) => (
                                <CommandItem
                                    key={outputOption}
                                    value={outputOption}
                                    onSelect={(newValue) => {
                                        if (newValue === currentValue) return;

                                        setSettings((prev) =>
                                            prev
                                                ? {
                                                      ...prev,
                                                      projectConfig: {
                                                          ...prev.projectConfig,
                                                          inspireOutput: newValue as InspireOutput,
                                                      },
                                                  }
                                                : null,
                                        );

                                        setOpen(false);
                                    }}
                                >
                                    {outputOption}
                                    <Check
                                        className={cn(
                                            "ml-auto",
                                            currentValue === outputOption ? "opacity-100" : "opacity-0",
                                        )}
                                    />
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}
