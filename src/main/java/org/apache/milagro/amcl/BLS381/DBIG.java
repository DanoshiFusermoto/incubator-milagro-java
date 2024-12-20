/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

/* AMCL double length DBIG number class */ 

package org.apache.milagro.amcl.BLS381;

public class DBIG {
	protected long[] w=new long[BIG.DNLEN];

/* normalise this */
	public void norm() {
		long d,carry=0;
		for (int i=0;i<BIG.DNLEN-1;i++)
		{
			d=w[i]+carry;
			carry=d>>BIG.BASEBITS;
			w[i]=d&BIG.BMASK;
		}
		w[BIG.DNLEN-1]=(w[BIG.DNLEN-1]+carry);
	}


/*
	public String toRawString()
	{
		DBIG b=new DBIG(this);
		String s="(";
		for (int i=0;i<BIG.DNLEN-1;i++)
		{
			s+=Long.toHexString(b.w[i]); s+=",";
		}
		s+=Long.toHexString(b.w[BIG.DNLEN-1]); s+=")";
		return s;
	}
*/

/* split DBIG at position n, return higher half, keep lower half */
	public BIG split(int n)
	{
		BIG t=BIG.createFast();
		int m=n%BIG.BASEBITS;
		long nw,carry=w[BIG.DNLEN-1]<<(BIG.BASEBITS-m);

		for (int i=BIG.DNLEN-2;i>=BIG.NLEN-1;i--)
		{
			nw=(w[i]>>m)|carry;
			carry=(w[i]<<(BIG.BASEBITS-m))&BIG.BMASK;
			t.w[i-BIG.NLEN+1]=nw;
			//t.set(i-BIG.NLEN+1,nw);
		}
		w[BIG.NLEN-1]&=(((long)1<<m)-1);
		return t;
	}

/****************************************************************************/

/* return number of bits in this */
	public int nbits() {
		int bts,k=BIG.DNLEN-1;
		long c;
		norm();
		while (w[k]==0 && k>=0) k--;
		if (k<0) return 0;
		bts=BIG.BASEBITS*k;
		c=w[k];
		while (c!=0) {c/=2; bts++;}
		return bts;
	}

/* convert this to string */
	public String toString() {
		DBIG b;
		String s="";
		int len=nbits();
		if (len%4==0) len>>=2; //len/=4;
		else {len>>=2; len++;}

		for (int i=len-1;i>=0;i--)
		{
			b=new DBIG(this);
			b.shr(i*4);
			s+=Integer.toHexString((int)(b.w[0]&15));
		}
		return s;
	}

	public void cmove(DBIG g,int d)
	{
		int i;
		for (i=0;i<BIG.DNLEN;i++)
		{
			w[i]^=(w[i]^g.w[i])&BIG.cast_to_chunk(-d);
		}
	}

/* Constructors */
	public static DBIG createFast()
	{
		return new DBIG();
	}
	
	private DBIG()
	{ }

	public DBIG(int x)
	{
		w[0]=x;
		for (int i=1;i<BIG.DNLEN;i++)
			w[i]=0;
	}

	public DBIG(DBIG x)
	{
		w[0]=x.w[0]; w[1]=x.w[1];
		w[2]=x.w[2]; w[3]=x.w[3];
		w[4]=x.w[4]; w[5]=x.w[5];
		w[6]=x.w[6]; w[7]=x.w[7];
		w[8]=x.w[8]; w[9]=x.w[9];
		w[10]=x.w[10]; w[11]=x.w[11];
		w[12]=x.w[12]; w[13]=x.w[13];
	}

	public DBIG(BIG x)
	{
		for (int i=0;i<BIG.NLEN-1;i++)
			w[i]=x.w[i]; //get(i);

		w[BIG.NLEN-1]=x.w[(BIG.NLEN-1)]&BIG.BMASK; /* top word normalized */
		w[BIG.NLEN]=(x.w[(BIG.NLEN-1)]>>BIG.BASEBITS);

		for (int i=BIG.NLEN+1;i<BIG.DNLEN;i++) w[i]=0;
	}

/* Copy from another DBIG */
	public void copy(DBIG x)
	{
		w[0]=x.w[0]; w[1]=x.w[1];
		w[2]=x.w[2]; w[3]=x.w[3];
		w[4]=x.w[4]; w[5]=x.w[5];
		w[6]=x.w[6]; w[7]=x.w[7];
		w[8]=x.w[8]; w[9]=x.w[9];
		w[10]=x.w[10]; w[11]=x.w[11];
		w[12]=x.w[12]; w[13]=x.w[13];
	}

/* Copy into upper part */
	public void ucopy(BIG x)
	{
		for (int i=0;i<BIG.NLEN;i++)
			w[i]=0;
		for (int i=BIG.NLEN;i<BIG.DNLEN;i++)
			w[i]=x.w[i-BIG.NLEN];
	}

/* test this=0? */
	public boolean iszilch() {
		for (int i=0;i<BIG.DNLEN;i++)
			if (w[i]!=0) return false;
		return true; 
	}

/* shift this right by k bits */
	public void shr(int k) {
		int n=k%BIG.BASEBITS;
		int m=k/BIG.BASEBITS;	
		for (int i=0;i<BIG.DNLEN-m-1;i++)
			w[i]=(w[m+i]>>n)|((w[m+i+1]<<(BIG.BASEBITS-n))&BIG.BMASK);
		w[BIG.DNLEN-m-1]=w[BIG.DNLEN-1]>>n;
		for (int i=BIG.DNLEN-m;i<BIG.DNLEN;i++) w[i]=0;
	}

/* shift this left by k bits */
	public void shl(int k) {
		int n=k%BIG.BASEBITS;
		int m=k/BIG.BASEBITS;

		w[BIG.DNLEN-1]=((w[BIG.DNLEN-1-m]<<n))|(w[BIG.DNLEN-m-2]>>(BIG.BASEBITS-n));
		for (int i=BIG.DNLEN-2;i>m;i--)
			w[i]=((w[i-m]<<n)&BIG.BMASK)|(w[i-m-1]>>(BIG.BASEBITS-n));
		w[m]=(w[0]<<n)&BIG.BMASK; 
		for (int i=0;i<m;i++) w[i]=0;
	}

/* this+=x */
	public void add(DBIG x) {
		for (int i=0;i<BIG.DNLEN;i++)
			w[i]+=x.w[i];	
	}

/* this-=x */
	public void sub(DBIG x) {
		for (int i=0;i<BIG.DNLEN;i++)
			w[i]-=x.w[i];
	}

	public void rsub(DBIG x) {
		for (int i=0;i<BIG.DNLEN;i++)
			w[i]=x.w[i]-w[i];
	}

/* Compare a and b, return 0 if a==b, -1 if a<b, +1 if a>b. Inputs must be normalised */
	public static int comp(DBIG a,DBIG b)
	{
		for (int i=BIG.DNLEN-1;i>=0;i--)
		{
			if (a.w[i]==b.w[i]) continue;
			if (a.w[i]>b.w[i]) return 1;
			else  return -1;
		}
		return 0;
	}

/* reduces this DBIG mod a BIG, and returns the BIG */
	public BIG mod(BIG c)
	{
		int k=0;  
		norm();
		DBIG m=new DBIG(c);
		DBIG r=DBIG.createFast();

		if (comp(this,m)<0) return new BIG(this);
		
		do
		{
			m.shl(1);
			k++;
		}
		while (comp(this,m)>=0);

		while (k>0)
		{
			m.shr(1);

			r.copy(this);
			r.sub(m);
			r.norm();
			cmove(r,(int)(1-((r.w[BIG.DNLEN-1]>>(BIG.CHUNK-1))&1)));

			k--;
		}
		return new BIG(this);
	}

/* return this/c */
	public BIG div(BIG c)
	{
		int d,k=0;
		DBIG m=new DBIG(c);
		DBIG dr=DBIG.createFast();
		BIG r=BIG.createFast();
		BIG a=BIG.createFast();
		BIG e=new BIG(1);
		norm();

		while (comp(this,m)>=0)
		{
			e.fshl(1);
			m.shl(1);
			k++;
		}

		while (k>0)
		{
			m.shr(1);
			e.shr(1);

			dr.copy(this);
			dr.sub(m);
			dr.norm();
			d=(int)(1-((dr.w[BIG.DNLEN-1]>>(BIG.CHUNK-1))&1));
			cmove(dr,d);
			r.copy(a);
			r.add(e);
			r.norm();
			a.cmove(r,d);
			k--;
		}
		return a;
	}
}
