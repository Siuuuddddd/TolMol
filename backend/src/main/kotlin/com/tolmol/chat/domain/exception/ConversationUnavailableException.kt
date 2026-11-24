package com.tolmol.chat.domain.exception

import com.tolmol.platform.domain.exception.BusinessException

class ConversationUnavailableException : BusinessException(404, "Conversation unavailable")
