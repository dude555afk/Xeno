                    val chunk = runCatching {
                        json.decodeFromString(StreamChunk.serializer(), data)
                    }.getOrNull() ?: continue