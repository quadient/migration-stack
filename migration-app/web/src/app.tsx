import { Button } from "@/components/ui/button";
import { Settings as SettingsIcon } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import SettingsDialog from "./dialogs/settings/settingsDialog.tsx";
import { useFetch } from "./hooks/useFetch.ts";
import { Separator } from "@/components/ui/separator.tsx";
import { Check, ChevronsUpDown } from "lucide-react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";
import type { Settings } from "@/dialogs/settings/settingsTypes.tsx";
import { useState } from "react";

function App() {
    return <AppLayout />;
}

type ScriptMetadata = {
    filename: string;
    category: string;
    path: string;
    sourceFormat: string | undefined;
    description: string | undefined;
}[];

function AppLayout() {
    const settingsResult = useFetch<Settings>("/api/settings");
    const scriptsResult = useFetch<ScriptMetadata>("/api/scripts");
    const [formatComboOpen, setFormatComboOpen] = useState(false);
    const [selectedFormat, setSelectedFormat] = useState<string | undefined>(undefined);

    const sourceFormats =
        scriptsResult.status === "ok"
            ? [...new Set(scriptsResult.data.map((script) => script.sourceFormat).filter(Boolean))]
            : undefined;

    return (
        <div className="min-h-screen grid grid-rows-[auto_1fr] px-[15%]">
            <div>
                <header className="pt-8">
                    <div className="flex justify-between items-center">
                        <div className="text-xl font-bold tracking-tight">Asset Migration Console</div>
                        <SettingsDialog
                            settingsResult={settingsResult}
                            trigger={
                                <Button variant="outline">
                                    <SettingsIcon className="w-4 h-4 mr-2" />
                                    Settings
                                </Button>
                            }
                        ></SettingsDialog>
                    </div>
                    <div className="text-gray-500 mt-2">Trigger processes for asset migration and deployment</div>
                </header>
                <Separator className="my-6" />
            </div>
            <main className="flex flex-1 min-h-0 gap-4">
                <section className="flex-1 overflow-y-auto">
                    <ChartCard />
                </section>
                <section className="flex-2 overflow-y-auto gap-4">
                    <div className="flex">
                        <div className="flex flex-1 text-lg font-semibold">Parse</div>
                        <Popover>
                            <PopoverTrigger asChild>
                                <Button
                                    variant="outline"
                                    role="combobox"
                                    aria-expanded={formatComboOpen}
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
                                                            if (newValue === selectedFormat) return;
                                                            setSelectedFormat(newValue);
                                                            setFormatComboOpen(false);
                                                        }}
                                                    >
                                                        {sourceFormat}
                                                        <Check
                                                            className={
                                                                "ml-auto " +
                                                                (selectedFormat === sourceFormat
                                                                    ? "opacity-100"
                                                                    : "opacity-0")
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
                    </div>
                    <div className="flex flex-wrap gap-4">
                        {scriptsResult.status === "ok" &&
                            scriptsResult.data.map((s) => (
                                <Card key={s.filename}>
                                    <CardHeader>
                                        <CardTitle className="text-xl">{s.filename.substring(0, 25)}</CardTitle>
                                    </CardHeader>
                                    <CardContent
                                        onClick={async () => {
                                            const result = await fetch("/api/scripts/run", {
                                                method: "POST",
                                                headers: {
                                                    "Content-Type": "application/json",
                                                },
                                                body: JSON.stringify({
                                                    path: s.path,
                                                }),
                                            });
                                            console.log(await result.text());
                                        }}
                                    >
                                        <p>Sup bro{s.description}</p>
                                    </CardContent>
                                </Card>
                            ))}
                    </div>
                </section>
            </main>
        </div>
    );
}

function ChartCard() {
    return (
        <Card className="w-full max-w-sm h-100 border-gray-200">
            <CardHeader>
                <CardTitle className="text-xl">Assets Parsed</CardTitle>
            </CardHeader>
            <CardContent>
                <p>TODO later</p>
            </CardContent>
        </Card>
    );
}

export default App;
