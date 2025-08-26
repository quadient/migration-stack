import { EmptyCard } from "@/utils/emptyCard.tsx";
import { FileCog } from "lucide-react";

export default function ChartsSection() {
    return (
        <section className="flex-1 min-h-0 overflow-y-auto">
            <EmptyCard message={"Run any parser to see asset statistics"} icon={FileCog} />
        </section>
    );
}
