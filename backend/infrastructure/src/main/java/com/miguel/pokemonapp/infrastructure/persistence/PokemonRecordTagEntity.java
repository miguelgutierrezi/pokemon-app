package com.miguel.pokemonapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "pokemon_record_tag")
@IdClass(PokemonRecordTagEntity.Pk.class)
class PokemonRecordTagEntity {

  @Id
  @Column(name = "record_id", nullable = false)
  private Long recordId;

  @Id
  @Column(name = "tag", nullable = false, length = 32)
  private String tag;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "record_id", nullable = false, insertable = false, updatable = false)
  private PokemonRecordEntity record;

  protected PokemonRecordTagEntity() {}

  PokemonRecordTagEntity(PokemonRecordEntity record, String tag) {
    this.record = record;
    this.recordId = record.getId();
    this.tag = tag;
  }

  @PrePersist
  @PreUpdate
  void syncRecordId() {
    if (record != null) {
      recordId = record.getId();
    }
  }

  String getTag() {
    return tag;
  }

  static final class Pk implements Serializable {
    private Long recordId;
    private String tag;

    protected Pk() {}

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Pk other)) {
        return false;
      }
      return Objects.equals(recordId, other.recordId) && Objects.equals(tag, other.tag);
    }

    @Override
    public int hashCode() {
      return Objects.hash(recordId, tag);
    }
  }
}
