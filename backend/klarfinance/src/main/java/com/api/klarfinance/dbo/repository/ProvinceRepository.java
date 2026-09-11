
package com.api.klarfinance.dbo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.dbo.model.Province;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

}
