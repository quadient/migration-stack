import { useSyncExternalStore } from "react";
import type { InputRequest } from "@/types/inputRequest.ts";

let current: InputRequest | undefined;
const listeners = new Set<() => void>();

function setCurrent(request: InputRequest | undefined) {
    current = request;
    listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

export function showInputRequest(request: InputRequest) {
    setCurrent(request);
}

export function clearInputRequest() {
    setCurrent(undefined);
}

export function useInputRequest(): InputRequest | undefined {
    return useSyncExternalStore(subscribe, () => current);
}

export async function sendInputAnswer(request: InputRequest, answer: string | null) {
    clearInputRequest();
    const body =
        answer === null ? { questionId: request.questionId, cancel: true } : { questionId: request.questionId, answer };
    try {
        const response = await fetch(`/api/job/${request.jobId}/input`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });
        if (!response.ok && response.status !== 409) {
            console.error("Failed to send answer:", await response.text());
        }
    } catch (error) {
        console.error("Failed to send answer:", error);
    }
}
