import {Button} from "@/components/ui/button"
import {Settings as SettingsIcon} from "lucide-react"
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import SettingsDialog from "./dialogs/settingsDialog.tsx"

function App() {
    return (
        <AppLayout/>
    )
}

function AppLayout() {
    return (
        <div className="min-h-screen grid grid-rows-[auto_1fr] px-[15%]">
            <header className="p-8 row-start-1 row-end-2">
                <div className="flex justify-between items-center">
                    <div className="text-xl font-bold tracking-tight">Asset Migration Console</div>
                    <SettingsDialog
                        trigger={
                            <Button variant="outline" className="hover:bg-gray-100">
                                <SettingsIcon className="w-4 h-4 mr-2"/>
                                Settings
                            </Button>
                        }>
                    </SettingsDialog>
                </div>
                <div className="text-gray-500 mt-2">
                    Trigger processes for asset migration and deployment
                </div>
            </header>
            <main className="grid grid-cols-[1fr_2fr] flex-1 row-start-2 row-end-3">
                <section className="p-4 flex justify-center"><ChartCard/></section>
                <section className="p-4 flex justify-center">Right Sectionn</section>
            </main>
        </div>
    )
}

function ChartCard() {
    return (<Card className="w-full max-w-sm h-100 border-gray-200">
        <CardHeader>
            <CardTitle className="text-xl">Assets Parsed</CardTitle>
        </CardHeader>
        <CardContent>
            <p>TODO later</p>
        </CardContent>
    </Card>)
}

export default App
