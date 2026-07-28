package com.anki.vocab_server.repository;

import com.anki.vocab_server.model.ProxyToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProxyTokenRepository extends CrudRepository<ProxyToken, UUID> {
    Iterable<ProxyToken> findByEnableTrue();
}
