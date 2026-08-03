package com.example.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ai.entity.Book;
import com.example.ai.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookMapper bookMapper;

    public List<Book> listAvailableBooks() {
        QueryWrapper<Book> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0);
        return bookMapper.selectList(wrapper);
    }
}