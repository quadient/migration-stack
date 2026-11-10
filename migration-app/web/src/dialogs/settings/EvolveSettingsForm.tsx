import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";
import { SettingsCombobox } from "@/dialogs/settings/SettingsCombobox.tsx";
import { type EvolveConfig, holderTypeOptions, type SettingsFormProps } from "@/dialogs/settings/settingsTypes.ts";

const defaultEvolveConfig: EvolveConfig = {
    apiRetryDelayMs: 1000,
    callTimeoutMs: 10_000,
    contentAuthorApiKey: "",
    companyUrl: "",
    holder: "",
    holderType: "userName",
    publishBlockActionId: "A_block_scenario_senior_template_designer_publish",
    initialBlockStateId: "S_block_scenario_assigned",
    publishTemplateActionId: "A_template_scenario_senior_template_designer_publish",
    initialTemplateStateId: "S_template_scenario_assigned",
    publishRuleActionId: "A_rule_scenario_senior_template_designer_publish",
    initialRuleStateId: "S_rule_scenario_assigned",
    publishStyleDefinitionActionId: "A_company_styles_scenario_scenario_senior_template_designer_publish",
    initialStyleDefinitionStateId: "S_company_styles_scenario_assigned",
    publishBaseTemplateActionId: "A_base_template_draft_scenario_senior_template_designer_publish",
};

export function EvolveSettingsForm({ settings, setSettings }: SettingsFormProps) {
    const evolveConfig = settings.migrationConfig.inspireConfig.evolveConfig ?? defaultEvolveConfig;

    const updateEvolveConfig = (key: keyof EvolveConfig, value: string | number | undefined) => {
        setSettings((prev) => ({
            ...prev,
            migrationConfig: {
                ...prev.migrationConfig,
                inspireConfig: {
                    ...prev.migrationConfig.inspireConfig,
                    evolveConfig: {
                        ...(prev.migrationConfig.inspireConfig.evolveConfig ?? defaultEvolveConfig),
                        [key]: value,
                    },
                },
            },
        }));
    };

    // Empty input removes the value, so the backend falls back to its default
    const updateOptional = (key: keyof EvolveConfig) => (value: string) => updateEvolveConfig(key, value || undefined);
    const updateOptionalNumber = (key: keyof EvolveConfig) => (value: string) =>
        updateEvolveConfig(key, value === "" ? undefined : Number(value));
    const updateRequired = (key: keyof EvolveConfig) => (value: string) => updateEvolveConfig(key, value);

    return (
        <Card>
            <CardHeader>
                <CardTitle>Evolve</CardTitle>
                <CardDescription>Content Author API connection and workflow configuration</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-6">
                <SettingsField
                    label="Company URL"
                    value={evolveConfig.companyUrl}
                    onChange={updateRequired("companyUrl")}
                />
                <SettingsField
                    label="Content Author API key"
                    type="password"
                    value={evolveConfig.contentAuthorApiKey}
                    onChange={updateRequired("contentAuthorApiKey")}
                />
                <SettingsField
                    label="Call timeout (ms)"
                    type="number"
                    placeholder="10000"
                    value={evolveConfig.callTimeoutMs}
                    onChange={updateOptionalNumber("callTimeoutMs")}
                />
                <SettingsField
                    label="API retry delay (ms)"
                    type="number"
                    placeholder="1000"
                    value={evolveConfig.apiRetryDelayMs}
                    onChange={updateOptionalNumber("apiRetryDelayMs")}
                />
                <SettingsField label="Holder" value={evolveConfig.holder} onChange={updateRequired("holder")} />
                <div className="grid gap-3">
                    <Label>Holder type</Label>
                    <SettingsCombobox
                        currentValue={evolveConfig.holderType}
                        onChange={updateRequired("holderType")}
                        options={holderTypeOptions}
                    />
                </div>
                <SettingsField
                    label="Template publish action ID"
                    value={evolveConfig.publishTemplateActionId}
                    onChange={updateRequired("publishTemplateActionId")}
                />
                <SettingsField
                    label="Template initial state ID"
                    placeholder="S_template_scenario_assigned"
                    value={evolveConfig.initialTemplateStateId}
                    onChange={updateOptional("initialTemplateStateId")}
                />
                <SettingsField
                    label="Block publish action ID"
                    value={evolveConfig.publishBlockActionId}
                    onChange={updateRequired("publishBlockActionId")}
                />
                <SettingsField
                    label="Block initial state ID"
                    placeholder="S_block_scenario_assigned"
                    value={evolveConfig.initialBlockStateId}
                    onChange={updateOptional("initialBlockStateId")}
                />
                <SettingsField
                    label="Rule publish action ID"
                    value={evolveConfig.publishRuleActionId}
                    onChange={updateRequired("publishRuleActionId")}
                />
                <SettingsField
                    label="Rule initial state ID"
                    placeholder="S_rule_scenario_assigned"
                    value={evolveConfig.initialRuleStateId}
                    onChange={updateOptional("initialRuleStateId")}
                />
                <SettingsField
                    label="Style definition publish action ID"
                    placeholder="Required to deploy styles"
                    value={evolveConfig.publishStyleDefinitionActionId}
                    onChange={updateOptional("publishStyleDefinitionActionId")}
                />
                <SettingsField
                    label="Style definition initial state ID"
                    placeholder="S_company_styles_scenario_assigned"
                    value={evolveConfig.initialStyleDefinitionStateId}
                    onChange={updateOptional("initialStyleDefinitionStateId")}
                />
                <SettingsField
                    label="Base template publish action ID"
                    placeholder="Required to deploy base templates"
                    value={evolveConfig.publishBaseTemplateActionId}
                    onChange={updateOptional("publishBaseTemplateActionId")}
                />
            </CardContent>
        </Card>
    );
}

type SettingsFieldProps = {
    label: string;
    value: string | number | undefined;
    onChange: (value: string) => void;
    type?: "text" | "password" | "number";
    placeholder?: string;
};

function SettingsField({ label, value, onChange, type = "text", placeholder }: SettingsFieldProps) {
    return (
        <div className="grid gap-3">
            <Label>{label}</Label>
            <Input
                type={type}
                placeholder={placeholder}
                value={value ?? ""}
                onChange={(e) => onChange(e.target.value)}
            />
        </div>
    );
}
