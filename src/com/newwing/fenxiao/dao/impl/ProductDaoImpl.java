package com.newwing.fenxiao.dao.impl;

import com.newwing.fenxiao.dao.impl.BaseDaoImpl;
import com.newwing.fenxiao.dao.IProductDao;
import com.newwing.fenxiao.entities.Product;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository("productDao")
@Scope("prototype")
public class ProductDaoImpl<T extends Product> extends BaseDaoImpl<T> implements IProductDao<T> {

}
