import React from "react";

export type UseRequestProps<T> = {
    url: string;
    onSuccess: (data: T) => void;
    onError: (error: unknown) => void;
    requestInit?: RequestInit;
    condition?: boolean;
};

export function useRequest<T>({ url, onSuccess, onError, requestInit, condition }: UseRequestProps<T>): void {
    const onSuccessEvent = React.useEffectEvent(onSuccess);
    const onErrorEvent = React.useEffectEvent(onError);
    const buildInit = React.useEffectEvent((signal: AbortSignal): RequestInit => ({
        ...requestInit,
        method: requestInit?.method || "GET",
        signal,
    }));

    React.useEffect(() => {
        if (condition === false) {
            return;
        }

        let cancelled = false;
        const controller = new AbortController();

        fetch(url, buildInit(controller.signal))
            .then((result) => {
                if (cancelled) return;
                result
                    .json()
                    .then((json) => {
                        if (cancelled) return;
                        onSuccessEvent(json as T);
                    })
                    .catch((err) => {
                        if (cancelled) return;
                        onErrorEvent(err);
                    });
            })
            .catch((err) => {
                if (cancelled) return;
                onErrorEvent(err);
            });

        return () => {
            controller.abort();
            cancelled = true;
        };
    }, [url, condition]);
}
