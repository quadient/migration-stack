import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { useState } from "react";
import { useFetch, type UseFetchResult } from "@/hooks/useFetch.ts";
import { FileCog } from "lucide-react";

type ModuleMetadata = {
    filename: string;
    category: string;
    path: string;
    displayName: string | undefined;
    sourceFormat: string | undefined;
    description: string | undefined;
}[];

export default function ModulesSection() {
    const [selectedFormat, setSelectedFormat] = useState<string | undefined>(undefined);

    const modulesResult = useFetch<ModuleMetadata>("/api/scripts");

    const sourceFormats = getSourceFormats(modulesResult);

    return (
        <section className="flex-2 overflow-y-auto">
            {modulesResult.status === "ok" && (
                <>
                    <div className="flex justify-between items-center pb-4">
                        <div className="flex flex-1 text-lg font-semibold">Parse</div>
                        <SourceFormatCombobox
                            selectedFormat={selectedFormat}
                            setSelectedFormat={setSelectedFormat}
                            sourceFormats={sourceFormats}
                        />
                    </div>
                    {!!selectedFormat && (
                        <div className="flex flex-wrap gap-4">
                            {modulesResult.data
                                .filter(
                                    (module) => module.category === "Parser" && module.sourceFormat === selectedFormat,
                                )
                                .map((module) => {
                                    const name = module.displayName || module.filename.replace(".groovy", "");
                                    return (
                                        <Card className="w-full max-w-sm h-75 flex flex-col" key={module.filename}>
                                            <CardHeader>
                                                <CardTitle className="flex items-center gap-2 text-lg font-normal">
                                                    <div className="bg-muted rounded-xl p-[10px]">
                                                        <FileCog className="w-6 h-6" />
                                                    </div>

                                                    {name}
                                                </CardTitle>
                                            </CardHeader>
                                            <CardContent className="text-muted-foreground">
                                                {module.description}
                                            </CardContent>
                                            <CardFooter className="flex justify-center mt-auto">
                                                <Button
                                                    type={"submit"}
                                                    onClick={() =>
                                                        onSaveChanges(module.path).then((result) => console.log(result))
                                                    }
                                                >
                                                    Execute Module
                                                </Button>
                                            </CardFooter>
                                        </Card>
                                    );
                                })}
                        </div>
                    )}
                </>
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

function getSourceFormats(scriptsResult: UseFetchResult<ModuleMetadata>): string[] | undefined {
    return scriptsResult.status === "ok"
        ? ([...new Set(scriptsResult.data.map((script) => script.sourceFormat).filter(Boolean))] as string[])
        : undefined;
}

async function onSaveChanges(path: string): Promise<void> {
    const response = await fetch("/api/scripts/run", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ path: path }),
    });
    if (!response.ok) {
        throw new Error("Failed to save settings");
    }
}
