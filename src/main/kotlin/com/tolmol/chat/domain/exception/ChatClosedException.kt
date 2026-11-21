package com.tolmol.chat.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class ChatClosedException : BusinessException(409, "Conversation is read-only")
