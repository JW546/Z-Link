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
    (taskDeclaration | pathPlanning | transportationState)* 
    RBRACE;

taskDeclaration: 
    annotation* TASK IDENTIFIER LBRACE fieldDeclaration* behaviorTree? RBRACE;

pathPlanning: 
    annotation* PATH IDENTIFIER LBRACE fieldDeclaration* RBRACE;

transportationState: 
    annotation* STATE IDENTIFIER LBRACE fieldDeclaration* RBRACE;

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