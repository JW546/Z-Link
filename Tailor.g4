grammar Tailor;

options {
    language = Java;
}

// 词法规则 - 注释优先
COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;

// 关键字必须在IDENTIFIER之前定义
TOPIC: 'topic';
TASK: 'task';
PATH: 'path';
STATE: 'state';
SEQUENCE: 'sequence';
SELECTOR: 'selector';
BEHAVIOR_TREE: 'behaviorTree';
UNIT_TYPE: 'UnitType';
DATA_EXCHANGE_CONTRACT: 'dataExchangeContract';
COMMAND_EXCHANGE_CONTRACT: 'commandExchangeContract';
DDS_IDL_BINDING: 'ddsIdlBinding';
DATA_FLOW: 'dataFlow';
COMMAND_FLOW: 'commandFlow';
BINDING: 'binding';
SAFETY_RULE: 'safetyRule';
FROM: 'from';
TO: 'to';
USES: 'uses';
IDL: 'idl';
FIELDS: 'fields';
QOS: 'qos';
PURPOSE: 'purpose';
KIND: 'kind';
EFFECT: 'effect';
BOOLEAN: 'true' | 'false';

// 标识符
IDENTIFIER: [\p{L}_][\p{L}\p{N}_]*;
STRING: '"' (~["\\] | '\\' .)* '"';  // 处理字符串
INT: [0-9]+;
DECIMAL: [0-9]+ '.' [0-9]+;
SEMICOLON: ';';
LBRACE: '{';
RBRACE: '}';
COLON: ':';
COMMA: ',';
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
AT: '@';
WS: [ \t\r\n]+ -> skip;  // 忽略空格和换行符

// 语法规则
tailorProgram: topicDeclaration+;

topicDeclaration: 
    TOPIC IDENTIFIER LBRACE 
    (taskDeclaration | pathPlanning | transportationState | dataExchangeContract | commandExchangeContract | ddsIdlBinding)* 
    RBRACE;

taskDeclaration: 
    annotation* TASK IDENTIFIER LBRACE fieldDeclaration* behaviorTree? RBRACE;

pathPlanning: 
    annotation* PATH IDENTIFIER LBRACE fieldDeclaration* RBRACE;

transportationState: 
    annotation* STATE IDENTIFIER LBRACE fieldDeclaration* RBRACE;

dataExchangeContract:
    DATA_EXCHANGE_CONTRACT LBRACE dataFlowDeclaration+ RBRACE;

dataFlowDeclaration:
    DATA_FLOW IDENTIFIER FROM endpoint TO endpoint TOPIC STRING USES qualifiedName
    FIELDS fieldList QOS IDENTIFIER PURPOSE STRING SEMICOLON;

commandExchangeContract:
    COMMAND_EXCHANGE_CONTRACT LBRACE (commandFlowDeclaration | safetyRuleDeclaration)+ RBRACE;

commandFlowDeclaration:
    COMMAND_FLOW IDENTIFIER FROM endpoint TO endpoint TOPIC STRING USES qualifiedName
    KIND IDENTIFIER FIELDS fieldList QOS IDENTIFIER EFFECT STRING SEMICOLON;

safetyRuleDeclaration:
    SAFETY_RULE IDENTIFIER EQUAL STRING SEMICOLON;

ddsIdlBinding:
    DDS_IDL_BINDING LBRACE ddsBindingDeclaration+ RBRACE;

ddsBindingDeclaration:
    BINDING IDENTIFIER TOPIC STRING USES qualifiedName IDL STRING QOS IDENTIFIER SEMICOLON;

endpoint:
    STRING | IDENTIFIER;

qualifiedName:
    IDENTIFIER ('.' IDENTIFIER)*;

fieldList:
    LBRACKET IDENTIFIER (COMMA IDENTIFIER)* RBRACKET;

annotation:
    AT IDENTIFIER LPAREN annotationValue? RPAREN;

annotationValue:
    annotationField (COMMA annotationField)* 
    | value=STRING 
    | value=IDENTIFIER
    | value=INT
    | value=DECIMAL
    | value=BOOLEAN;

annotationField:
    IDENTIFIER '=' (literalValue | quantityValue);

fieldDeclaration: 
    annotation* fieldType=IDENTIFIER? fieldName=IDENTIFIER (COLON | EQUAL) fieldValue SEMICOLON;

EQUAL: '=';

fieldValue: 
    literalValue
    | quantityValue;

literalValue:
    STRING
    | INT
    | DECIMAL
    | BOOLEAN
    | IDENTIFIER;

quantityValue:
    (INT | DECIMAL) unitType STRING;

unitType: 
    AT UNIT_TYPE;

behaviorTree:
    BEHAVIOR_TREE LBRACE btNode* RBRACE;

btNode: sequence | selector | action;

sequence:
    SEQUENCE LPAREN? LBRACE btNode+ RBRACE RPAREN?;

selector:
    SELECTOR LPAREN? LBRACE btNode+ RBRACE RPAREN?;

action:
    IDENTIFIER (LPAREN actionParams? RPAREN)?;

actionParams: 
    actionParam (COMMA actionParam)*;

actionParam: 
    IDENTIFIER COLON (literalValue | quantityValue);
