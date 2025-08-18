import {
    Dialog,
    DialogContent,
    DialogTrigger,
    DialogHeader,
    DialogTitle,
    DialogDescription
} from "@/components/ui/dialog.tsx"
import type {ReactNode} from "react";

type SettingsDialogProps = {
    trigger: ReactNode;
};

export default function SettingsDialog({trigger}: SettingsDialogProps) {
    return (
        <Dialog>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent className="bg-white border-0">
                <DialogHeader>
                    <DialogTitle>Settings</DialogTitle>
                    <DialogDescription className="text-gray-500">
                        Configure project settings and connection to services
                    </DialogDescription>
                </DialogHeader>
            </DialogContent>
        </Dialog>
    );
}