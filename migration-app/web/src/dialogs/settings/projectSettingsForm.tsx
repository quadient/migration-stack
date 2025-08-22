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
} from "@/dialogs/settings/settingsTypes.tsx";
import { cn } from "@/lib/utils.ts";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";

type ProjectSettingsFormProps = {
    settings: Settings;
    setSettings: (value: ((prevState: Settings | null) => Settings | null) | Settings | null) => void;
};

export function ProjectSettingsForm({ settings, setSettings }: ProjectSettingsFormProps) {
    const updateSettings = (key: keyof ProjectConfig, value: string) => {
        setSettings((prev) => (prev ? { ...prev, projectConfig: { ...prev.projectConfig, [key]: value } } : null));
    };

    return (
        <div className="grid gap-5">
            <div className="font-bold">Project Settings</div>
            <div className="grid gap-4">
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
                <div className="grid gap-3">
                    <Label>Default variable structure</Label>
                    <Input
                        value={settings.projectConfig.defaultVariableStructure ?? ""}
                        onChange={(e) => updateSettings("defaultVariableStructure", e.target.value)}
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Default target folder</Label>
                    <Input
                        value={settings.projectConfig.defaultTargetFolder ?? ""}
                        onChange={(e) => updateSettings("defaultTargetFolder", e.target.value)}
                    />
                </div>
                <div className="font-normal border-b pb-5 mb-10">Paths</div>
                <div className="grid gap-3">
                    <Label>Images</Label>
                    <Input
                        value={settings.projectConfig.paths.images ?? ""}
                        onChange={(e) =>
                            setSettings((prev) =>
                                prev
                                    ? {
                                          ...prev,
                                          projectConfig: {
                                              ...prev.projectConfig,
                                              paths: {
                                                  images: e.target.value,
                                              },
                                          },
                                      }
                                    : null,
                            )
                        }
                    />
                </div>
            </div>
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
            <PopoverContent className="p-0">
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
