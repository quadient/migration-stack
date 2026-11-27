import { useState } from "react";
import { Input } from "@/components/ui/input.tsx";
import { errorMessage } from "@/common/configApi.ts";

type DraftTextFieldProps<T> = {
    value: T;
    format: (value: T) => string;
    parse: (text: string) => T;
    onCommit: (value: T) => void;
    onValidityChange?: (valid: boolean) => void;
    multiline?: boolean;
};

// Keeps the typed text locally and only parses it when the field loses focus,
// so values that are invalid while typing (half-written JSON, a trailing comma) can still be entered.
export function DraftTextField<T>({
    value,
    format,
    parse,
    onCommit,
    onValidityChange,
    multiline = false,
}: DraftTextFieldProps<T>) {
    const [text, setText] = useState(() => format(value));
    const [shownValue, setShownValue] = useState(value);
    const [error, setError] = useState<string | null>(null);

    if (value !== shownValue) {
        setShownValue(value);
        setText(format(value));
        setError(null);
    }

    const commit = () => {
        try {
            onCommit(parse(text));
            setError(null);
            onValidityChange?.(true);
        } catch (e) {
            setError(errorMessage(e));
            onValidityChange?.(false);
        }
    };

    return (
        <div className="grid gap-2">
            {multiline ? (
                <textarea
                    className="min-h-48 w-full rounded-md border bg-transparent px-3 py-2 font-mono text-sm text-foreground shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    onBlur={commit}
                />
            ) : (
                <Input value={text} onChange={(e) => setText(e.target.value)} onBlur={commit} />
            )}
            {error && <p className="text-sm text-destructive">{error}</p>}
        </div>
    );
}
