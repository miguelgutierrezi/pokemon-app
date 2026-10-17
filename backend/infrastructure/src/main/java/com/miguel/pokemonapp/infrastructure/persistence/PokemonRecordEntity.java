package com.miguel.pokemonapp.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pokemon_record")
class PokemonRecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "national_number", nullable = false, unique = true)
  private int nationalNumber;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "category")
  private String category;

  @Column(name = "height_m", nullable = false, precision = 4, scale = 1)
  private BigDecimal heightM;

  @Column(name = "weight_kg", nullable = false, precision = 6, scale = 1)
  private BigDecimal weightKg;

  @Column(name = "image_url", length = 1024)
  private String imageUrl;

  @Column(name = "generation", nullable = false, length = 64)
  private String generation;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  @Column(name = "localized_name", length = 100)
  private String localizedName;

  @Column(name = "location", length = 200)
  private String location;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("slot ASC")
  private List<PokemonRecordTypeEntity> types = new ArrayList<>();

  @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PokemonRecordTagEntity> tags = new ArrayList<>();

  protected PokemonRecordEntity() {}

  Long getId() {
    return id;
  }

  int getNationalNumber() {
    return nationalNumber;
  }

  void setNationalNumber(int nationalNumber) {
    this.nationalNumber = nationalNumber;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  String getCategory() {
    return category;
  }

  void setCategory(String category) {
    this.category = category;
  }

  BigDecimal getHeightM() {
    return heightM;
  }

  void setHeightM(BigDecimal heightM) {
    this.heightM = heightM;
  }

  BigDecimal getWeightKg() {
    return weightKg;
  }

  void setWeightKg(BigDecimal weightKg) {
    this.weightKg = weightKg;
  }

  String getImageUrl() {
    return imageUrl;
  }

  void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  String getGeneration() {
    return generation;
  }

  void setGeneration(String generation) {
    this.generation = generation;
  }

  Instant getSyncedAt() {
    return syncedAt;
  }

  void setSyncedAt(Instant syncedAt) {
    this.syncedAt = syncedAt;
  }

  String getLocalizedName() {
    return localizedName;
  }

  void setLocalizedName(String localizedName) {
    this.localizedName = localizedName;
  }

  String getLocation() {
    return location;
  }

  void setLocation(String location) {
    this.location = location;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  List<PokemonRecordTypeEntity> getTypes() {
    return types;
  }

  List<PokemonRecordTagEntity> getTags() {
    return tags;
  }

  void replaceTypes(List<String> typeSlugs) {
    types.clear();
    int slot = 1;
    for (String type : typeSlugs) {
      types.add(new PokemonRecordTypeEntity(this, slot++, type));
    }
  }

  void replaceTags(List<String> tagValues) {
    tags.clear();
    for (String tag : tagValues) {
      tags.add(new PokemonRecordTagEntity(this, tag));
    }
  }
}
