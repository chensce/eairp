/*
 * Copyright 2023-2033 WanSen AI Team, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://opensource.wansenai.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.wansenai.service.receipt.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wansenai.dto.receipt.QueryReceiptDTO;
import com.wansenai.entities.basic.Customer;
import com.wansenai.entities.basic.Member;
import com.wansenai.entities.basic.Supplier;
import com.wansenai.entities.receipt.*;
import com.wansenai.service.basic.CustomerService;
import com.wansenai.service.basic.MemberService;
import com.wansenai.service.basic.SupplierService;
import com.wansenai.service.product.ProductService;
import com.wansenai.service.product.ProductStockKeepUnitService;
import com.wansenai.service.receipt.*;
import com.wansenai.service.user.ISysUserService;
import com.wansenai.utils.constants.CommonConstants;
import com.wansenai.utils.enums.BaseCodeEnum;
import com.wansenai.utils.response.Response;
import com.wansenai.vo.receipt.ReceiptDetailVO;
import com.wansenai.vo.receipt.ReceiptVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRetailService receiptRetailService;

    private final ReceiptRetailSubService receiptRetailSubService;

    private final ReceiptSaleService receiptSaleService;

    private final ReceiptSaleSubService receiptSaleSubService;

    private final ReceiptPurchaseService receiptPurchaseService;

    private final ReceiptPurchaseSubService receiptPurchaseSubService;

    private final MemberService memberService;

    private final CustomerService customerService;

    private final SupplierService supplierService;

    private final ISysUserService userService;

    private final ProductService productService;

    private final ProductStockKeepUnitService productStockKeepUnitService;

    public ReceiptServiceImpl(ReceiptRetailService receiptRetailService, ReceiptRetailSubService receiptRetailSubService, ReceiptSaleService receiptSaleService, ReceiptSaleSubService receiptSaleSubService, ReceiptPurchaseService receiptPurchaseService, ReceiptPurchaseSubService receiptPurchaseSubService, MemberService memberService, CustomerService customerService, SupplierService supplierService, ISysUserService userService, ProductService productService, ProductStockKeepUnitService productStockKeepUnitService) {
        this.receiptRetailService = receiptRetailService;
        this.receiptRetailSubService = receiptRetailSubService;
        this.receiptSaleService = receiptSaleService;
        this.receiptSaleSubService = receiptSaleSubService;
        this.receiptPurchaseService = receiptPurchaseService;
        this.receiptPurchaseSubService = receiptPurchaseSubService;
        this.memberService = memberService;
        this.customerService = customerService;
        this.supplierService = supplierService;
        this.userService = userService;
        this.productService = productService;
        this.productStockKeepUnitService = productStockKeepUnitService;
    }

    @Override
    public Response<Page<ReceiptVO>> otherReceipt(QueryReceiptDTO receiptDTO) {
        String type = receiptDTO.getType();
        String subType = receiptDTO.getSubType();
        if (StringUtils.hasLength(type) && StringUtils.hasLength(subType)) {
            switch (type) {
                case "零售":
                    return Response.responseData(getReceiptRetailVOList(receiptDTO));
                case "销售":
                    return Response.responseData(getReceiptSaleVOList(receiptDTO));
                case "采购":
                    return Response.responseData(getReceiptPurchaseVOList(receiptDTO));
                default:
                    break;
            }
        }

        return Response.responseMsg(BaseCodeEnum.PARAMETER_NULL);
    }

    @Override
    public Response<Page<ReceiptDetailVO>> getOtherDetail(QueryReceiptDTO receiptDTO) {
        String type = receiptDTO.getType();
        Long id = receiptDTO.getId();
        if (StringUtils.hasLength(type) && id != null) {
            switch (type) {
                case "零售":
                    return Response.responseData(getReceiptRetailDetailVOList(receiptDTO));
                case "销售":
                    return Response.responseData(getReceiptSaleDetailVOList(receiptDTO));
                case "采购":
                    return Response.responseData(getReceiptPurchaseDetailVOList(receiptDTO));
                default:
                    break;
            }
        }

        return Response.responseMsg(BaseCodeEnum.PARAMETER_NULL);
    }

    private Page<ReceiptVO> getReceiptRetailVOList(QueryReceiptDTO receiptDTO) {
        var result = new Page<ReceiptVO>();

        var pageData = new Page<ReceiptRetailMain>(receiptDTO.getPage(), receiptDTO.getPageSize());
        var receiptRetailVOList = receiptRetailService.lambdaQuery()
                .eq(ReceiptRetailMain::getSubType, receiptDTO.getSubType())
                .eq(ReceiptRetailMain::getDeleteFlag, CommonConstants.NOT_DELETED)
                .eq(ReceiptRetailMain::getStatus, CommonConstants.REVIEWED)
                .page(pageData);

        if (receiptRetailVOList.getRecords().isEmpty()) {
            return null;
        }

        var receiptVos = new ArrayList<ReceiptVO>(receiptRetailVOList.getRecords().size() + 2);
        receiptRetailVOList.getRecords().forEach(item -> {
            var member = memberService.lambdaQuery()
                    .eq(Member::getId, item.getMemberId())
                    .eq(Member::getDeleteFlag, CommonConstants.NOT_DELETED)
                    .one();

            var operator = userService.getById(item.getCreateBy());

            var productNumber = receiptRetailSubService.lambdaQuery()
                    .eq(ReceiptRetailSub::getReceiptMainId, item.getId())
                    .list()
                    .stream()
                    .mapToInt(ReceiptRetailSub::getProductNumber)
                    .sum();

            ReceiptVO receiptVO = ReceiptVO.builder()
                    .id(item.getId())
                    .name(Optional.ofNullable(member.getMemberName()).orElse(""))
                    .receiptNumber(item.getReceiptNumber())
                    .receiptDate(item.getReceiptDate())
                    .operator(Optional.ofNullable(operator.getName()).orElse(""))
                    .productNumber(productNumber)
                    .totalAmount(item.getTotalAmount())
                    .status(item.getStatus())
                    .build();
            receiptVos.add(receiptVO);
        });
        result.setRecords(receiptVos);
        result.setPages(receiptRetailVOList.getPages());
        result.setSize(receiptRetailVOList.getSize());
        result.setTotal(receiptRetailVOList.getTotal());
        result.setCurrent(receiptRetailVOList.getCurrent());
        return result;
    }

    private Page<ReceiptVO> getReceiptSaleVOList(QueryReceiptDTO receiptDTO) {
        var result = new Page<ReceiptVO>();
        var pageData = new Page<ReceiptSaleMain>(receiptDTO.getPage(), receiptDTO.getPageSize());

        var receiptSaleVOList = receiptSaleService.lambdaQuery()
                .eq(ReceiptSaleMain::getSubType, receiptDTO.getSubType())
                .eq(ReceiptSaleMain::getDeleteFlag, CommonConstants.NOT_DELETED)
                .eq(ReceiptSaleMain::getStatus, CommonConstants.REVIEWED)
                .page(pageData);

        if (receiptSaleVOList.getRecords().isEmpty()) {
            return null;
        }

        var receiptVos = new ArrayList<ReceiptVO>(receiptSaleVOList.getRecords().size() + 2);
        receiptSaleVOList.getRecords().forEach(item -> {
            var customer = customerService.lambdaQuery()
                    .eq(Customer::getId, item.getCustomerId())
                    .eq(Customer::getDeleteFlag, CommonConstants.NOT_DELETED)
                    .one();

            var operator = userService.getById(item.getCreateBy());

            var productNumber = receiptSaleSubService.lambdaQuery()
                    .eq(ReceiptSaleSub::getReceiptSaleMainId, item.getId())
                    .list()
                    .stream()
                    .mapToInt(ReceiptSaleSub::getProductNumber)
                    .sum();

            ReceiptVO receiptVO = ReceiptVO.builder()
                    .id(item.getId())
                    .name(Optional.ofNullable(customer.getCustomerName()).orElse(""))
                    .receiptNumber(item.getReceiptNumber())
                    .receiptDate(item.getReceiptDate())
                    .operator(Optional.ofNullable(operator.getName()).orElse(""))
                    .productNumber(productNumber)
                    .totalAmount(item.getTotalAmount())
                    .taxRateTotalAmount(item.getDiscountLastAmount())
                    .status(item.getStatus())
                    .build();
            receiptVos.add(receiptVO);
        });
        result.setRecords(receiptVos);
        result.setPages(receiptSaleVOList.getPages());
        result.setSize(receiptSaleVOList.getSize());
        result.setTotal(receiptSaleVOList.getTotal());
        result.setCurrent(receiptSaleVOList.getCurrent());
        return result;
    }

    private Page<ReceiptVO> getReceiptPurchaseVOList(QueryReceiptDTO receiptDTO) {
        var result = new Page<ReceiptVO>();
        var pageData = new Page<ReceiptPurchaseMain>(receiptDTO.getPage(), receiptDTO.getPageSize());

        var receiptPurchaseVOList = receiptPurchaseService.lambdaQuery()
                .eq(ReceiptPurchaseMain::getSubType, receiptDTO.getSubType())
                .eq(ReceiptPurchaseMain::getDeleteFlag, CommonConstants.NOT_DELETED)
                .eq(ReceiptPurchaseMain::getStatus, CommonConstants.REVIEWED)
                .page(pageData);

        if (receiptPurchaseVOList.getRecords().isEmpty()) {
            return null;
        }

        var receiptVos = new ArrayList<ReceiptVO>(receiptPurchaseVOList.getRecords().size() + 2);
        receiptPurchaseVOList.getRecords().forEach(item -> {
            var supplier = supplierService.lambdaQuery()
                    .eq(Supplier::getId, item.getSupplierId())
                    .eq(Supplier::getDeleteFlag, CommonConstants.NOT_DELETED)
                    .one();

            var operator = userService.getById(item.getCreateBy());

            var productNumber = receiptPurchaseSubService.lambdaQuery()
                    .eq(ReceiptPurchaseSub::getReceiptPurchaseMainId, item.getId())
                    .list()
                    .stream()
                    .mapToInt(ReceiptPurchaseSub::getProductNumber)
                    .sum();

            ReceiptVO receiptVO = ReceiptVO.builder()
                    .id(item.getId())
                    .name(Optional.ofNullable(supplier.getSupplierName()).orElse(""))
                    .receiptNumber(item.getReceiptNumber())
                    .receiptDate(item.getReceiptDate())
                    .operator(Optional.ofNullable(operator.getName()).orElse(""))
                    .productNumber(productNumber)
                    .totalAmount(item.getTotalAmount())
                    .taxRateTotalAmount(item.getDiscountLastAmount())
                    .status(item.getStatus())
                    .build();
            receiptVos.add(receiptVO);
        });

        result.setRecords(receiptVos);
        result.setPages(receiptPurchaseVOList.getPages());
        result.setSize(receiptPurchaseVOList.getSize());
        result.setTotal(receiptPurchaseVOList.getTotal());
        result.setCurrent(receiptPurchaseVOList.getCurrent());
        return result;
    }

    private Page<ReceiptDetailVO> getReceiptRetailDetailVOList(QueryReceiptDTO receiptDTO) {
        var result = new Page<ReceiptDetailVO>();
        var pageData = new Page<ReceiptRetailSub>(receiptDTO.getPage(), receiptDTO.getPageSize());

        var receiptRetailDetails = receiptRetailSubService.lambdaQuery()
                .eq(ReceiptRetailSub::getReceiptMainId, receiptDTO.getId())
                .page(pageData);

        if (receiptRetailDetails.getRecords().isEmpty()) {
            return null;
        }

        var receiptDetailVos = new ArrayList<ReceiptDetailVO>(receiptRetailDetails.getRecords().size() + 2);
        receiptRetailDetails.getRecords().forEach(item -> {

            var receiptDetailVO = ReceiptDetailVO.builder()
                    .id(item.getId())
                    .warehouseId(item.getWarehouseId())
                    .productId(item.getProductId())
                    .productBarcode(item.getProductBarcode())
                    .unitPrice(item.getUnitPrice())
                    .productNumber(item.getProductNumber())
                    .amount(item.getTotalAmount())
                    .remark(item.getRemark())
                    .build();

            var productVO = productService.getProductInfoDetail(item.getProductId()).getData();
            if (productVO != null) {
                receiptDetailVO.setProductName(productVO.getProductName());
                receiptDetailVO.setProductStandard(productVO.getProductStandard());
                receiptDetailVO.setProductModel(productVO.getProductModel());
                receiptDetailVO.setUnit(productVO.getProductUnit());
            }
            // 查询库存
            var stock = productStockKeepUnitService.getProductByBarCode(item.getProductBarcode(), item.getWarehouseId());
            if (stock != null && stock.getData() != null) {
                receiptDetailVO.setStock(stock.getData().getStock());
            }

            receiptDetailVos.add(receiptDetailVO);
        });

        result.setRecords(receiptDetailVos);
        result.setPages(receiptRetailDetails.getPages());
        result.setSize(receiptRetailDetails.getSize());
        result.setTotal(receiptRetailDetails.getTotal());
        result.setCurrent(receiptRetailDetails.getCurrent());

        return result;
    }

    private Page<ReceiptDetailVO> getReceiptSaleDetailVOList(QueryReceiptDTO receiptDTO) {
        var result = new Page<ReceiptDetailVO>();
        var pageData = new Page<ReceiptSaleSub>(receiptDTO.getPage(), receiptDTO.getPageSize());

        var receiptSaleDetails = receiptSaleSubService.lambdaQuery()
                .eq(ReceiptSaleSub::getReceiptSaleMainId, receiptDTO.getId())
                .page(pageData);

        if (receiptSaleDetails.getRecords().isEmpty()) {
            return null;
        }

        var receiptDetailVos = new ArrayList<ReceiptDetailVO>(receiptSaleDetails.getRecords().size() + 2);
        receiptSaleDetails.getRecords().forEach(item -> {
            var receiptDetailVO = ReceiptDetailVO.builder()
                    .id(item.getId())
                    .warehouseId(item.getWarehouseId())
                    .productId(item.getProductId())
                    .productBarcode(item.getProductBarcode())
                    .productNumber(item.getProductNumber())
                    .amount(item.getTotalAmount())
                    .unitPrice(item.getUnitPrice())
                    .taxRate(item.getTaxRate())
                    .taxAmount(item.getTaxAmount())
                    .taxIncludedAmount(item.getTaxIncludedAmount())
                    .remark(item.getRemark())
                    .build();

            var productVO = productService.getProductInfoDetail(item.getProductId()).getData();
            if (productVO != null) {
                receiptDetailVO.setProductName(productVO.getProductName());
                receiptDetailVO.setProductStandard(productVO.getProductStandard());
                receiptDetailVO.setProductModel(productVO.getProductModel());
                receiptDetailVO.setUnit(productVO.getProductUnit());
            }
            // 查询库存
            var stock = productStockKeepUnitService.getProductByBarCode(item.getProductBarcode(), item.getWarehouseId());
            if (stock != null && stock.getData() != null) {
                receiptDetailVO.setStock(stock.getData().getStock());
            }
            receiptDetailVos.add(receiptDetailVO);
        });

        result.setRecords(receiptDetailVos);
        result.setPages(receiptSaleDetails.getPages());
        result.setSize(receiptSaleDetails.getSize());
        result.setTotal(receiptSaleDetails.getTotal());
        result.setCurrent(receiptSaleDetails.getCurrent());

        return result;
    }

    private Page<ReceiptDetailVO> getReceiptPurchaseDetailVOList(QueryReceiptDTO receiptDTO) {
        var result = new Page<ReceiptDetailVO>();
        var pageData = new Page<ReceiptPurchaseSub>(receiptDTO.getPage(), receiptDTO.getPageSize());

        var receiptPurchaseDetails = receiptPurchaseSubService.lambdaQuery()
                .eq(ReceiptPurchaseSub::getReceiptPurchaseMainId, receiptDTO.getId())
                .page(pageData);

        if (receiptPurchaseDetails.getRecords().isEmpty()) {
            return null;
        }

        var receiptDetailVos = new ArrayList<ReceiptDetailVO>(receiptPurchaseDetails.getRecords().size() + 2);
        receiptPurchaseDetails.getRecords().forEach(item -> {
            var receiptDetailVO = ReceiptDetailVO.builder()
                    .id(item.getId())
                    .warehouseId(item.getWarehouseId())
                    .productId(item.getProductId())
                    .productBarcode(item.getProductBarcode())
                    .productNumber(item.getProductNumber())
                    .amount(item.getTotalAmount())
                    .unitPrice(item.getUnitPrice())
                    .taxRate(item.getTaxRate())
                    .taxAmount(item.getTaxAmount())
                    .taxIncludedAmount(item.getTaxIncludedAmount())
                    .remark(item.getRemark())
                    .build();
            var productVO = productService.getProductInfoDetail(item.getProductId()).getData();
            if (productVO != null) {
                receiptDetailVO.setProductName(productVO.getProductName());
                receiptDetailVO.setProductStandard(productVO.getProductStandard());
                receiptDetailVO.setProductModel(productVO.getProductModel());
                receiptDetailVO.setUnit(productVO.getProductUnit());
            }
            // 查询库存
            var stock = productStockKeepUnitService.getProductByBarCode(item.getProductBarcode(), item.getWarehouseId());
            if (stock != null && stock.getData() != null) {
                receiptDetailVO.setStock(stock.getData().getStock());
            }
            receiptDetailVos.add(receiptDetailVO);
        });
        result.setRecords(receiptDetailVos);
        result.setPages(receiptPurchaseDetails.getPages());
        result.setSize(receiptPurchaseDetails.getSize());
        result.setTotal(receiptPurchaseDetails.getTotal());
        result.setCurrent(receiptPurchaseDetails.getCurrent());

        return result;
    }
}
