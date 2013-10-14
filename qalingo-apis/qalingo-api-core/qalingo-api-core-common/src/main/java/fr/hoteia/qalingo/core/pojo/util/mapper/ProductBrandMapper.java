package fr.hoteia.qalingo.core.pojo.util.mapper;

import fr.hoteia.qalingo.core.domain.ProductBrand;
import fr.hoteia.qalingo.core.pojo.ProductBrandPojo;
import org.springframework.stereotype.Component;

@Component("productBrandMapper")
public class ProductBrandMapper extends AbstractPojoMapper<ProductBrand, ProductBrandPojo> {

    @Override
    public Class<ProductBrand> getObjectType() {
        return ProductBrand.class;
    }

    @Override
    public Class<ProductBrandPojo> getPojoType() {
        return ProductBrandPojo.class;
    }
}
