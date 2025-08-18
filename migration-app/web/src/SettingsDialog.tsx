import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
} from "@/components/ui/sheet"
import {Button} from "@/components/ui/button"

export type SettingsDialogProps = {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export default function SettingsDialog({open, onOpenChange}: SettingsDialogProps) {
    return <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent side="right" id="settings-sheet" className="bg-white w-screen max-w-none h-svh p-0 flex flex-col">
            <SheetHeader>
                <SheetTitle>Settings</SheetTitle>
                <SheetDescription>Configure migration options.</SheetDescription>
            </SheetHeader>

            <div className="py-4 space-y-4">
                <Button variant="secondary" onClick={() => onOpenChange(false)}>
                    Close
                </Button>
            </div>
        </SheetContent>
    </Sheet>
}