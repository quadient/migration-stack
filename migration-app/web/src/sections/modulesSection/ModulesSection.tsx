import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import React, { useState } from "react";
import { useFetch, type UseFetchResult } from "@/hooks/useFetch.ts";
import { FileCog, Rocket, Play, LoaderCircle } from "lucide-react";

type ModuleMetadata = {
    filename: string;
    category: string;
    path: string;
    displayName: string | undefined;
    sourceFormat: string | undefined;
    description: string | undefined;
};

export default function ModulesSection() {
    const [selectedFormat, setSelectedFormat] = useState<string | undefined>(undefined);

    const modulesResult = useFetch<ModuleMetadata[]>("/api/scripts");

    const sourceFormats = getSourceFormats(modulesResult);

    return (
        <section className="flex-2 overflow-y-auto">
            {modulesResult.status === "ok" && (
                <div className="grid gap-4">
                    <div className="flex justify-between items-center">
                        <div className="flex flex-1 text-lg font-semibold">Parse</div>
                        <SourceFormatCombobox
                            selectedFormat={selectedFormat}
                            setSelectedFormat={setSelectedFormat}
                            sourceFormats={sourceFormats}
                        />
                    </div>
                    {!!selectedFormat && (
                        <ModuleRow
                            modules={modulesResult.data.filter(
                                (module) => module.category === "Parser" && module.sourceFormat === selectedFormat,
                            )}
                            icon={<FileCog className="w-6 h-6" />}
                        />
                    )}
                    <div className="flex flex-1 text-lg font-semibold">Deploy</div>
                    <ModuleRow
                        modules={modulesResult.data.filter(
                            (module) =>
                                module.category === "Deployment" && module.filename === "DeployDocumentObjects.groovy",
                        )}
                        icon={<Rocket className="w-6 h-6" />}
                    />
                </div>
            )}
        </section>
    );
}

type SourceFormatComboboxProps = {
    selectedFormat: string | undefined;
    setSelectedFormat: (format: string) => void;
    sourceFormats: string[] | undefined;
};

function SourceFormatCombobox({ selectedFormat, setSelectedFormat, sourceFormats }: SourceFormatComboboxProps) {
    const [open, setOpen] = useState(false);

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={open}
                    className="w-75 justify-between font-normal"
                >
                    {selectedFormat ?? "Select Source Format"}
                    <ChevronsUpDown className="opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-75 p-0">
                {sourceFormats && (
                    <Command>
                        <CommandList>
                            <CommandGroup>
                                {sourceFormats.map((sourceFormat) => (
                                    <CommandItem
                                        key={sourceFormat}
                                        value={sourceFormat}
                                        onSelect={(newValue) => {
                                            if (newValue !== selectedFormat) {
                                                setSelectedFormat(newValue);
                                            }
                                            setOpen(false);
                                        }}
                                    >
                                        {sourceFormat}
                                        <Check
                                            className={
                                                "ml-auto " +
                                                (selectedFormat === sourceFormat ? "opacity-100" : "opacity-0")
                                            }
                                        />
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        </CommandList>
                    </Command>
                )}
            </PopoverContent>
        </Popover>
    );
}

function ModuleRow({ modules, icon }: { modules: ModuleMetadata[]; icon: React.ReactNode }) {
    return (
        <div className="flex flex-wrap gap-4">
            {modules.map((module) => {
                return <ModuleCard module={module} icon={icon} />;
            })}
        </div>
    );
}

function ModuleCard({ module, icon }: { module: ModuleMetadata; icon: React.ReactNode }) {
    const [running, setRunning] = useState(false);

    const name = module.displayName || module.filename.replace(".groovy", "");
    return (
        <Card className="w-full max-w-sm h-75 flex flex-col" key={module.filename}>
            <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg font-normal">
                    <div className="bg-muted rounded-xl p-[10px]">{icon}</div>
                    {name}
                </CardTitle>
            </CardHeader>
            <CardContent className="text-muted-foreground">{module.description}</CardContent>
            <CardFooter className="flex justify-center mt-auto">
                <Button className="w-50" type={"submit"} onClick={() => handleExecuteModule(module.path, setRunning)}>
                    {running ? (
                        <>
                            <LoaderCircle className="animate-spin" />
                            Processing...
                        </>
                    ) : (
                        <>
                            <Play className="mr-1" />
                            Execute Module
                        </>
                    )}
                </Button>
            </CardFooter>
        </Card>
    );
}

function getSourceFormats(scriptsResult: UseFetchResult<ModuleMetadata[]>): string[] | undefined {
    return scriptsResult.status === "ok"
        ? ([...new Set(scriptsResult.data.map((script) => script.sourceFormat).filter(Boolean))] as string[])
        : undefined;
}

async function handleExecuteModule(path: string, setRunning: (value: boolean) => void): Promise<void> {
    setRunning(true);
    try {
        await fetch("/api/scripts/run", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ path: path }),
        });
    } catch (error) {
        console.error("Error executing module:", error);
    } finally {
        setRunning(false);
    }
}
