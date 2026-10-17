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
@Table(name = "pokemon_record_type")
@IdClass(PokemonRecordTypeEntity.Pk.class)
class PokemonRecordTypeEntity {

  @Id
  @Column(name = "record_id", nullable = false)
  private Long recordId;

  @Id
  @Column(name = "slot", nullable = false)
  private int slot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "record_id", nullable = false, insertable = false, updatable = false)
  private PokemonRecordEntity record;

  @Column(name = "type", nullable = false, length = 64)
  private String type;

  protected PokemonRecordTypeEntity() {}

  PokemonRecordTypeEntity(PokemonRecordEntity record, int slot, String type) {
    this.record = record;
    this.recordId = record.getId();
    this.slot = slot;
    this.type = type;
  }

  @PrePersist
  @PreUpdate
  void syncRecordId() {
    if (record != null) {
      recordId = record.getId();
    }
  }

  int getSlot() {
    return slot;
  }

  String getType() {
    return type;
  }

  static final class Pk implements Serializable {
    private Long recordId;
    private int slot;

    protected Pk() {}

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Pk other)) {
        return false;
      }
      return slot == other.slot && Objects.equals(recordId, other.recordId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(recordId, slot);
    }
  }
}
