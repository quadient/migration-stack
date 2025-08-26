import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useState } from "react";
import { useFetch, type UseFetchResult } from "@/hooks/useFetch.ts";
import { FileCog, Rocket, Play, LoaderCircle, FileText } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { EmptyCard } from "@/utils/emptyCard.tsx";
import LogsDialog from "@/dialogs/logs/logsDialog.tsx";

type ModuleMetadata = {
    filename: string;
    category: string;
    path: string;
    displayName: string | undefined;
    sourceFormat: string | undefined;
    description: string | undefined;
};

export type RunResult = {
    path: string;
    name: string;
    status: "running" | "success" | "error";
    lastUpdated: Date;
    logs: string[];
};

export type ScriptRunResultsMap = Map<string, RunResult>;

type SetRunResults = (value: (prev: ScriptRunResultsMap) => ScriptRunResultsMap) => void;

type ModulesSectionProp = {
    scriptRunResults: ScriptRunResultsMap;
    setScriptRunResults: SetRunResults;
};

export default function ModulesSection({ scriptRunResults, setScriptRunResults }: ModulesSectionProp) {
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
                                    .map((module) => (
                                        <ModuleCard
                                            key={module.path}
                                            module={module}
                                            icon={FileCog}
                                            runResult={scriptRunResults.get(module.path)}
                                            setRunResults={setScriptRunResults}
                                        />
                                    ))
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
                                    return (
                                        <ModuleCard
                                            key={module.path}
                                            module={module}
                                            icon={Rocket}
                                            runResult={scriptRunResults.get(module.path)}
                                            setRunResults={setScriptRunResults}
                                        />
                                    );
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
                    className="w-60 justify-between font-normal"
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
                                            className={`ml-auto ${selectedFormat === sourceFormat ? "opacity-100" : "opacity-0"}`}
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

type ModuleCardProps = {
    module: ModuleMetadata;
    icon: LucideIcon;
    runResult: RunResult | undefined;
    setRunResults: SetRunResults;
};

function ModuleCard({ module, icon: Icon, runResult, setRunResults }: ModuleCardProps) {
    return (
        <Card className="w-full max-w-sm min-w-75 h-75 flex flex-col" key={module.filename}>
            <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg font-normal">
                    <div className="bg-muted rounded-xl p-2.5">
                        <Icon className="w-6 h-6" />
                    </div>
                    {getName(module)}
                </CardTitle>
            </CardHeader>
            <CardContent className="text-muted-foreground">{module.description}</CardContent>
            <CardFooter className="flex flex-col gap-4 justify-center mt-auto">
                {(runResult?.status === "success" || runResult?.status === "error") && (
                    <div className="flex justify-between items-center w-full">
                        <div className="text-muted-foreground text-xs">
                            {`Last run: ${runResult.lastUpdated.toLocaleString()}`}
                        </div>
                        <LogsDialog
                            trigger={
                                <Button className="text-muted-foreground text-xs" variant={"ghost"}>
                                    <FileText className="text-muted-foreground" />
                                    View logs
                                </Button>
                            }
                            runResult={runResult}
                        />
                    </div>
                )}
                <Button className="w-50" type={"submit"} onClick={() => handleExecuteModule(module, setRunResults)}>
                    {runResult?.status === "running" ? (
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

async function handleExecuteModule(module: ModuleMetadata, setRunResults: SetRunResults): Promise<void> {
    const path = module.path;
    const name = getName(module);

    setRunResults((prev) =>
        new Map(prev).set(path, { path, name, status: "running", lastUpdated: new Date(), logs: [] }),
    );

    try {
        const response = await fetch("/api/scripts/run", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ path }),
        });
        const { logs } = await response.json();

        setRunResults((prev) =>
            new Map(prev).set(path, { path, name, status: "success", lastUpdated: new Date(), logs }),
        );
    } catch (error) {
        console.error("Error executing module:", error);
        setRunResults((prev) =>
            new Map(prev).set(path, { path, name, status: "error", lastUpdated: new Date(), logs: [String(error)] }),
        );
    }
}

function getName(module: ModuleMetadata): string {
    return module.displayName || module.filename.replace(".groovy", "");
}

function getSourceFormats(scriptsResult: UseFetchResult<ModuleMetadata[]>): string[] | undefined {
    return scriptsResult.status === "ok"
        ? ([...new Set(scriptsResult.data.map((script) => script.sourceFormat).filter(Boolean))] as string[])
        : undefined;
}
