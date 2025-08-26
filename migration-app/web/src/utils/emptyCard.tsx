import { Card, CardContent } from "@/components/ui/card.tsx";
import type { LucideIcon } from "lucide-react";

export function EmptyCard({ icon: Icon, message }: { icon: LucideIcon; message: string }) {
    return (
        <Card className="max-w-sm h-75 justify-center p-10">
            <CardContent className="flex flex-col items-center">
                <div className="bg-muted rounded-full p-5">
                    <Icon className="w-12 h-12 text-gray-400" />
                </div>
                <text className="text-center text-muted-foreground pt-2">{message}</text>
            </CardContent>
        </Card>
    );
}
