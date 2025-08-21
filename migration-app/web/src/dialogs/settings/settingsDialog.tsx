import { useEffect, useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogTrigger,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter,
} from "@/components/ui/dialog.tsx";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import type { ReactNode } from "react";
import * as React from "react";
import { type InspireOutput, inspireOutputOptions, type Settings } from "@/dialogs/settings/settingsTypes.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils.ts";

type SettingsDialogProps = {
    trigger: ReactNode;
};

export default function SettingsDialog({ trigger }: SettingsDialogProps) {
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
                        Configure project settings and connection to services
                    </DialogDescription>
                </DialogHeader>
                {settings && (
                    <div className="max-h-120 overflow-y-auto pr-4">{renderSettings(settings, setSettings)}</div>
                )}
                <DialogFooter>
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

function renderSettings(
    settings: Settings,
    setSettings: (value: ((prevState: Settings | null) => Settings | null) | Settings | null) => void,
): React.ReactNode {
    return (
        <div className="grid gap-5">
            <div className="font-bold">Project Settings</div>
            <div className="grid gap-4">
                <div className="grid gap-3">
                    <Label>Name</Label>
                    <Input
                        value={settings.projectConfig.name}
                        onChange={(e) =>
                            setSettings((prev) =>
                                prev
                                    ? {
                                          ...prev,
                                          projectConfig: {
                                              ...prev.projectConfig,
                                              name: e.target.value,
                                          },
                                      }
                                    : null,
                            )
                        }
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Input data path</Label>
                    <Input
                        value={settings.projectConfig.inputDataPath}
                        onChange={(e) =>
                            setSettings((prev) =>
                                prev
                                    ? {
                                          ...prev,
                                          projectConfig: {
                                              ...prev.projectConfig,
                                              inputDataPath: e.target.value,
                                          },
                                      }
                                    : null,
                            )
                        }
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
                                onChange={(e) =>
                                    setSettings((prev) =>
                                        prev
                                            ? {
                                                  ...prev,
                                                  projectConfig: {
                                                      ...prev.projectConfig,
                                                      interactiveTenant: e.target.value,
                                                  },
                                              }
                                            : null,
                                    )
                                }
                            />
                        </div>
                        <div className="grid gap-3">
                            <Label>Base template path</Label>
                            <Input
                                value={settings.projectConfig.baseTemplatePath}
                                onChange={(e) =>
                                    setSettings((prev) =>
                                        prev
                                            ? {
                                                  ...prev,
                                                  projectConfig: {
                                                      ...prev.projectConfig,
                                                      baseTemplatePath: e.target.value,
                                                  },
                                              }
                                            : null,
                                    )
                                }
                            />
                        </div>
                    </>
                )}
                {settings.projectConfig.inspireOutput === "Designer" && (
                    <div className="grid gap-3">
                        <Label>Source Base template path</Label>
                        <Input
                            value={settings.projectConfig.sourceBaseTemplatePath ?? ""}
                            onChange={(e) =>
                                setSettings((prev) =>
                                    prev
                                        ? {
                                              ...prev,
                                              projectConfig: {
                                                  ...prev.projectConfig,
                                                  sourceBaseTemplatePath: e.target.value,
                                              },
                                          }
                                        : null,
                                )
                            }
                        />
                    </div>
                )}
                <div className="grid gap-3">
                    <Label>Default variable structure</Label>
                    <Input
                        value={settings.projectConfig.defaultVariableStructure ?? ""}
                        onChange={(e) =>
                            setSettings((prev) =>
                                prev
                                    ? {
                                          ...prev,
                                          projectConfig: {
                                              ...prev.projectConfig,
                                              defaultVariableStructure: e.target.value,
                                          },
                                      }
                                    : null,
                            )
                        }
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Default target folder</Label>
                    <Input
                        value={settings.projectConfig.defaultTargetFolder ?? ""}
                        onChange={(e) =>
                            setSettings((prev) =>
                                prev
                                    ? {
                                          ...prev,
                                          projectConfig: {
                                              ...prev.projectConfig,
                                              defaultTargetFolder: e.target.value,
                                          },
                                      }
                                    : null,
                            )
                        }
                    />
                </div>
                <div className="font-normal">Paths</div>
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
