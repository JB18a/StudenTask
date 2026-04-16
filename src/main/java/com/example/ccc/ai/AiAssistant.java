package com.example.ccc.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface AiAssistant {

    @SystemMessage("""
            你是本系统内置的智能助手。你的任务是用简体中文帮助用户完成与系统数据相关的问题解答。

            规则：
            - 当用户问题需要查询任务、提交记录、用户信息等系统数据时，优先调用可用的工具函数获取数据，再基于结果作答。
            - 不要编造数据库中不存在的内容；如查不到数据，请明确说明“未查询到”并给出下一步建议（例如让用户提供 taskId/userId/关键词）。
            - 回答尽量结构化、可执行；需要列表时用条目。
            """)
    TokenStream chat(@MemoryId Long memoryId, @UserMessage String message);
}

