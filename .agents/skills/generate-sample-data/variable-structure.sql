-- Raw variable structure entries for sample-data.mjs (path resolution happens in the script).
-- psql variables: :'proj' = project name, :'vs' = variable structure id.
-- Output: one JSON document { project, structureId, structureName, languageVariable, variables: [...] }
-- variables[]: { id, name, dataType, literalPath, refId }
--   name = structure entry name -> variable name -> variable id (same fallback as the deploy)
--   exactly one of literalPath / refId is set; dataType is null when the variable row is missing
SELECT jsonb_build_object(
               'project', vs.project_name,
               'structureId', vs.id,
               'structureName', vs.name,
               'languageVariable', vs.language_variable,
               'variables', coalesce(jsonb_agg(jsonb_build_object(
                       'id', e.key,
                       'name', coalesce(e.value ->> 'name', v.name, e.key),
                       'dataType', v.data_type,
                       'literalPath', CASE jsonb_typeof(e.value -> 'path')
                                          WHEN 'string' THEN e.value ->> 'path'
                                          ELSE e.value -> 'path' ->> 'path' END,
                       'refId', e.value -> 'path' ->> 'variableId'
                                               ) ORDER BY e.key) FILTER (WHERE e.key IS NOT NULL), '[]'::jsonb))
FROM variable_structure vs
         LEFT JOIN LATERAL jsonb_each(vs.structure) e ON TRUE
         LEFT JOIN variable v ON v.id = e.key AND v.project_name = vs.project_name
WHERE vs.project_name = :'proj'
  AND vs.id = :'vs'
GROUP BY vs.project_name, vs.id, vs.name, vs.language_variable;
