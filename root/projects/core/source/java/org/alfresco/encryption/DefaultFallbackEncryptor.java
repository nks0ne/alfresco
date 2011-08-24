/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.encryption;

import java.io.InputStream;
import java.io.Serializable;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.Pair;

// TODO for decryption, ensure that we catch invalidkeyexception rather than AlfrescoRuntimeException
/**
 * @since 4.0
 */
public class DefaultFallbackEncryptor implements FallbackEncryptor
{
	private Encryptor fallback;
	private Encryptor main;

	public void setFallback(Encryptor fallback)
	{
		this.fallback = fallback;
	}

	public void setMain(Encryptor main)
	{
		this.main = main;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public Pair<byte[], AlgorithmParameters> encrypt(String keyAlias,
			AlgorithmParameters params, byte[] input)
	{
		// Note: encrypt supported only for main encryptor
		Pair<byte[], AlgorithmParameters> ret = main.encrypt(keyAlias, params, input);
		return ret;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public byte[] decrypt(String keyAlias, AlgorithmParameters params,
			byte[] input)
	{
		byte[] ret;

		// for decryption, try the main encryptor. If that fails (possibly as a result of the keys being updated),
		// fall back to fallback encryptor.
		try
		{
			ret = main.decrypt(keyAlias, params, input);
		}
		catch(AlfrescoRuntimeException e)
		{
			ret = fallback.decrypt(keyAlias, params, input);
		}

		return ret;
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
	public InputStream decrypt(String keyAlias, AlgorithmParameters params,
			InputStream in)
	{
		InputStream ret;

		// for decryption, try the main encryptor. If that fails (possibly as a result of the keys being updated),
		// fall back to fallback encryptor.
		try
		{
			ret = main.decrypt(keyAlias, params, in);
		}
		catch(AlfrescoRuntimeException e)
		{
			ret = fallback.decrypt(keyAlias, params, in);
		}

		return ret;
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
	public Pair<byte[], AlgorithmParameters> encryptObject(String keyAlias,
			AlgorithmParameters params, Object input)
	{
		// Note: encrypt supported only for main encryptor
		Pair<byte[], AlgorithmParameters> ret = main.encryptObject(keyAlias, params, input);
		return ret;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public Object decryptObject(String keyAlias, AlgorithmParameters params,
			byte[] input)
	{
		Object ret;

		// for decryption, try the main encryptor. If that fails (possibly as a result of the keys being updated),
		// fall back to fallback encryptor.
		try
		{
			ret = main.decryptObject(keyAlias, params, input);
		}
		catch(AlfrescoRuntimeException e)
		{
			ret = fallback.decryptObject(keyAlias, params, input);
		}

		return ret;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public Serializable sealObject(String keyAlias, AlgorithmParameters params,
			Serializable input)
	{
		// Note: encrypt supported only for main encryptor
		Serializable ret = main.sealObject(keyAlias, params, input);
		return ret;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public Serializable unsealObject(String keyAlias, Serializable input)
			throws InvalidKeyException
	{
		Serializable ret;

		// for decryption, try the main encryptor. If that fails (possibly as a result of the keys being updated),
		// fall back to fallback encryptor.
		try
		{
			ret = main.unsealObject(keyAlias, input);
		}
		catch(AlfrescoRuntimeException e)
		{
			ret = fallback.unsealObject(keyAlias, input);
		}

		return ret;

	}

    /**
     * {@inheritDoc}
     */
    @Override
	public AlgorithmParameters decodeAlgorithmParameters(byte[] encoded)
	{
		AlgorithmParameters ret;

		try
		{
			ret = main.decodeAlgorithmParameters(encoded);
		}
		catch(AlfrescoRuntimeException e)
		{
			ret = fallback.decodeAlgorithmParameters(encoded);
		}

		return ret;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean available(String keyAlias)
	{
		// always available
		return true;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public boolean isFallbackAvailable()
	{
		return fallback.available(KeyProvider.ALIAS_METADATA);
	}
}