export type SettingsFormProps = {
    settings: Settings;
    setSettings: (value: ((prevState: Settings) => Settings) | Settings) => void;
};

export type Settings = {
    projectConfig: ProjectConfig;
    migrationConfig: MigrationConfig;
    sourceFormat?: string;
};

export type MigrationConfig = {
    dbConfig: DbConfig;
    inspireConfig: InspireConfig;
    storageRoot?: string | undefined;
};

export type DbConfig = {
    host: string;
    port: number;
    dbName: string;
    user: string;
    password: string;
};

export type InspireConfig = {
    ipsConfig: IpsConfig;
    evolveConfig: EvolveConfig | null;
};

export type EvolveConfig = {
    apiRetryDelayMs?: number;
    callTimeoutMs?: number;
    contentAuthorApiKey: string;
    companyUrl: string;
    holder: string;
    holderType: string;
    publishBlockActionId: string;
    initialBlockStateId?: string;
    publishTemplateActionId: string;
    initialTemplateStateId?: string;
    publishRuleActionId: string;
    initialRuleStateId?: string;
    publishStyleDefinitionActionId?: string;
    initialStyleDefinitionStateId?: string;
    publishBaseTemplateActionId?: string;
};

export const holderTypeOptions = ["userName", "userGroup"];

export type IpsConfig = {
    host: string;
    port: number;
    timeoutSeconds: number;
};

export type ProjectConfig = {
    name: string;
    baseTemplatePath: string;
    styleDefinitionPath: string | undefined;
    inputDataPath: string;
    interactiveTenant: string;
    defaultTargetFolder?: string;
    paths: PathsConfig;
    inspireOutput: InspireOutput;
    sourceBaseTemplatePath?: string;
    defaultVariableStructure?: string;
    defaultLanguage?: string;
    selectedDocumentObjectsFile?: string;
    selectedDocumentObjects?: string[];
    subProjectId?: string;
    context: Record<string, unknown>;
};

export type PathsConfig = {
    images?: string | undefined;
    fonts?: string | undefined;
    documents?: string | undefined;
    attachments?: string | undefined;
};

export const inspireOutputOptions = ["Designer", "Interactive", "Evolve"] as const;
export type InspireOutput = (typeof inspireOutputOptions)[number];
