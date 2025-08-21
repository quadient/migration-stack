import { Button } from "@/components/ui/button";
import { Settings as SettingsIcon } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import SettingsDialog from "./dialogs/settings/settingsDialog.tsx";
import { useFetch } from "./hooks/useFetch.ts";

function App() {
    return <AppLayout />;
}

type ScriptMetadata = { filename: string; category: string; description: string | undefined }[];

function AppLayout() {
    const scriptsResult = useFetch<ScriptMetadata>("/api/scripts", []);

    return (
        <div className="min-h-screen grid grid-rows-[auto_1fr] px-[15%]">
            <header className="p-8 row-start-1 row-end-2">
                <div className="flex justify-between items-center">
                    <div className="text-xl font-bold tracking-tight">Asset Migration Console</div>
                    <SettingsDialog
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
            <main className="grid grid-cols-[1fr_2fr] flex-1 row-start-2 row-end-3">
                <section className="p-4 flex justify-center">
                    <ChartCard />
                </section>
                <section className="p-4 flex justify-center">Right Section</section>
                {scriptsResult.status === "ok" &&
                    scriptsResult.data
                        .filter((s) => s.category === "migration deploy")
                        .map((s) => {
                            return (
                                <Card key={s.filename} className="w-full max-w-sm h-100 border-gray-200">
                                    <CardHeader>
                                        <CardTitle className="text-xl">{s.filename.substring(0, 25)}</CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <p>{s.description}</p>
                                    </CardContent>
                                </Card>
                            );
                        })}
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
