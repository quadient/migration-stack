import { CircleX } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";

type ErrorDialogProps = {
    title: string;
    error: string | undefined;
    onClose: () => void;
};

export default function ErrorDialog({ title, error, onClose }: ErrorDialogProps) {
    return (
        <Dialog modal open={!!error} onOpenChange={(open) => !open && onClose()}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <CircleX className="size-5 shrink-0 text-status-fail" />
                        {title}
                    </DialogTitle>
                    <DialogDescription className="break-words">{error}</DialogDescription>
                </DialogHeader>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline">Close</Button>
                    </DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
