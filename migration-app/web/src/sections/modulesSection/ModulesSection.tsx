import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useState } from "react";
import { useFetch, type UseFetchResult } from "@/hooks/useFetch.ts";
import { FileCog, Rocket, Play, LoaderCircle } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { EmptyCard } from "@/utils/emptyCard.tsx";

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
        <ScrollArea className="flex-2">
            {modulesResult.status === "ok" && (
                <div className="flex gap-6">
                    <div className="flex flex-col flex-1 gap-4">
                        <div className="flex items-center h-8 gap-4">
                            <div className="text-lg font-semibold">Parse</div>
                            <SourceFormatCombobox
                                selectedFormat={selectedFormat}
                                setSelectedFormat={setSelectedFormat}
                                sourceFormats={sourceFormats}
                            />
                        </div>
                        <div className="flex flex-row gap-4 flex-wrap">
                            {selectedFormat ? (
                                modulesResult.data
                                    .filter(
                                        (module) =>
                                            module.category === "Parser" && module.sourceFormat === selectedFormat,
                                    )
                                    .map((module) => {
                                        return <ModuleCard module={module} icon={FileCog} />;
                                    })
                            ) : (
                                <EmptyCard icon={FileCog} message={"Select source format to see available parsers"} />
                            )}
                        </div>
                    </div>
                    <div className="flex flex-col flex-1 gap-4">
                        <div className="flex items-center h-8 gap-4">
                            <div className="text-lg font-semibold">Deploy</div>
                        </div>
                        <div className="flex flex-row gap-4 flex-wrap">
                            {modulesResult.data
                                .filter(
                                    (module) =>
                                        module.category === "Deployment" &&
                                        module.filename === "DeployDocumentObjects.groovy",
                                )
                                .map((module) => {
                                    return <ModuleCard module={module} icon={Rocket} />;
                                })}
                        </div>
                    </div>
                </div>
            )}
        </ScrollArea>
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
                    className="w-50 justify-between font-normal"
                >
                    {selectedFormat ?? "Select Source Format"}
                    <ChevronsUpDown className="opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-50 p-0">
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

function ModuleCard({ module, icon: Icon }: { module: ModuleMetadata; icon: LucideIcon }) {
    const [running, setRunning] = useState(false);

    const name = module.displayName || module.filename.replace(".groovy", "");
    return (
        <Card className="w-full max-w-sm h-75 flex flex-col" key={module.filename}>
            <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg font-normal">
                    <div className="bg-muted rounded-xl p-2.5">
                        <Icon className="w-6 h-6" />
                    </div>
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
