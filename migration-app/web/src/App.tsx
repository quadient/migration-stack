import {Button} from "@/components/ui/button"

function App() {
    return (
        <AppLayout/>
    )
}

function AppLayout() {
    return (
        <div className="min-h-screen grid grid-rows-[auto_1fr]">
            <header className="flex justify-between items-center p-4 row-start-1 row-end-2">
                <div className="text-lg font-bold">Migration</div>
                <Button variant="outline">Settings</Button>
            </header>
            <main className="grid grid-cols-2 flex-1 row-start-2 row-end-3">
                <section className="p-4">Left Section</section>
                <section className="p-4">Right Sectionn</section>
            </main>
        </div>
    )
}

export default App
