export type InputKind = "confirm" | "choose" | "chooseOrType";

export type InputRequest = {
    jobId: string;
    questionId: string;
    kind: InputKind;
    message: string;
    options: string[];
    default: string | null;
};
